package com.manoogianmedia.studiorack.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.UUID

class StudioRackRepository(
    private val context: Context,
    private val dao: StudioRackDao,
    private val tokenStore: TokenStore,
    private val client: SyncClient,
) {
    fun signedIn() = tokenStore.isSignedIn()
    fun records(type: String): Flow<List<CachedRecord>> = dao.observeRecords(type)
    fun supporting(type: String): Flow<List<SupportingRecord>> = dao.observeSupporting(type)
    fun cachedAttachments(): Flow<List<CachedAttachment>> = dao.observeCachedAttachments()
    fun syncState(): Flow<SyncState?> = dao.observeSyncState()
    fun pendingCount(): Flow<Int> = dao.observePendingCount()
    fun conflicts(): Flow<List<SyncConflict>> = dao.observeConflicts()

    suspend fun reportOverview(): JSONObject = client.reportOverview()

    suspend fun runAiReport(question: String): JSONObject = client.runAiReport(question)

    suspend fun saveSetList(
        setListId: String,
        setList: JSONObject,
        sections: List<Pair<String, JSONObject>>,
        entries: List<Pair<String, JSONObject>>,
    ) {
        val desired = buildList {
            add(Triple("set_list", setListId, setList))
            sections.forEach { add(Triple("set_list_section", it.first, it.second)) }
            entries.forEach { add(Triple("set_list_entry", it.first, it.second)) }
        }
        val existingChildren = (dao.records("set_list_section") + dao.records("set_list_entry"))
            .filter { JSONObject(it.json).optString("set_list_id") == setListId }
        val desiredKeys = desired.mapTo(mutableSetOf()) { it.first to it.second }
        val removed = existingChildren.filter { it.entityType to it.entityId !in desiredKeys }
        val existingByKey = (existingChildren + listOfNotNull(dao.record("set_list", setListId)))
            .associateBy { it.entityType to it.entityId }
        val orderedChanges = mutableListOf<PendingMutation>()
        var sequence = System.currentTimeMillis()

        desired.forEach { (type, id, json) ->
            val current = existingByKey[type to id]
            orderedChanges += mutation(type, id, "upsert", current?.revision ?: 0, json.toString(), sequence++)
        }
        // Child rows must be removed before their parent section.
        removed.sortedBy { if (it.entityType == "set_list_entry") 0 else 1 }.forEach { current ->
            orderedChanges += mutation(current.entityType, current.entityId, "delete", current.revision, current.json, sequence++)
        }
        dao.applyLocalBundle(
            upserts = desired.map { (type, id, json) -> CachedRecord(type, id, existingByKey[type to id]?.revision ?: 0, json.toString()) },
            deletes = removed.map { RecordRef(it.entityType, it.entityId) },
            mutations = orderedChanges,
        )
        syncNow()
    }

    suspend fun deleteSetList(setListId: String) {
        val children = (dao.records("set_list_entry") + dao.records("set_list_section"))
            .filter { JSONObject(it.json).optString("set_list_id") == setListId }
            .sortedBy { if (it.entityType == "set_list_entry") 0 else 1 }
        val root = dao.record("set_list", setListId) ?: return
        var sequence = System.currentTimeMillis()
        val records = children + root
        dao.applyLocalBundle(
            upserts = emptyList(),
            deletes = records.map { RecordRef(it.entityType, it.entityId) },
            mutations = records.map { mutation(it.entityType, it.entityId, "delete", it.revision, it.json, sequence++) },
        )
        syncNow()
    }

    private fun mutation(type: String, id: String, operation: String, revision: Int, json: String, createdAt: Long) = PendingMutation(
        mutationId = "mutation_${UUID.randomUUID().toString().replace("-", "")}",
        entityType = type,
        entityId = id,
        operation = operation,
        baseRevision = revision,
        json = json,
        createdAt = createdAt,
    )

    suspend fun save(entityType: String, entityId: String, data: JSONObject) {
        val current = dao.record(entityType, entityId)
        val revision = current?.revision ?: 0
        dao.putRecords(listOf(CachedRecord(entityType, entityId, revision, data.toString())))
        dao.removePendingForEntity(entityType, entityId)
        dao.putPending(
            PendingMutation(
                mutationId = "mutation_${UUID.randomUUID().toString().replace("-", "")}",
                entityType = entityType,
                entityId = entityId,
                operation = "upsert",
                baseRevision = revision,
                json = data.toString(),
            )
        )
        syncNow()
    }

    suspend fun delete(entityType: String, entityId: String) {
        val current = dao.record(entityType, entityId) ?: return
        dao.deleteRecord(entityType, entityId)
        dao.removePendingForEntity(entityType, entityId)
        dao.putPending(
            PendingMutation(
                mutationId = "mutation_${UUID.randomUUID().toString().replace("-", "")}",
                entityType = entityType,
                entityId = entityId,
                operation = "delete",
                baseRevision = current.revision,
                json = current.json,
            )
        )
        syncNow()
    }

    suspend fun resolveConflict(conflict: SyncConflict, keepLocal: Boolean) {
        if (keepLocal) {
            if (conflict.operation == "delete") dao.deleteRecord(conflict.entityType, conflict.entityId)
            else dao.putRecords(listOf(CachedRecord(conflict.entityType, conflict.entityId, conflict.serverRevision, conflict.localJson)))
            dao.putPending(
                PendingMutation(
                    mutationId = "mutation_${UUID.randomUUID().toString().replace("-", "")}",
                    entityType = conflict.entityType,
                    entityId = conflict.entityId,
                    operation = conflict.operation,
                    baseRevision = conflict.serverRevision,
                    json = conflict.localJson,
                )
            )
        } else if (conflict.serverJson == null) {
            dao.deleteRecord(conflict.entityType, conflict.entityId)
        } else {
            dao.putRecords(listOf(CachedRecord(conflict.entityType, conflict.entityId, conflict.serverRevision, conflict.serverJson)))
        }
        dao.removeConflict(conflict.mutationId)
        if (keepLocal) syncNow()
    }

    suspend fun signIn(email: String, accessCode: String, mfaCode: String) {
        val stableId = tokenStore.deviceId() ?: "android_" + Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val response = client.signIn(email, accessCode, mfaCode, "${Build.MANUFACTURER} ${Build.MODEL}", stableId)
        tokenStore.save(response.getString("access_token"), response.getString("device_id"), response.getString("account_id"))
        sync()
        scheduleAutomaticSync()
    }

    suspend fun signOut() {
        runCatching { client.revoke() }
        tokenStore.clear()
    }

    suspend fun sync() {
        pushPending()
        var cursor = dao.syncState()?.cursor ?: 0
        do {
            val response = client.pull(cursor)
            applyPull(response)
            cursor = response.getLong("cursor")
        } while (response.optBoolean("has_more", false))
        refreshAttachmentCache()
        refreshImageCache()
    }

    fun syncNow() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
    }

    fun scheduleAutomaticSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("studiorack-sync", ExistingPeriodicWorkPolicy.KEEP, work)
    }

    private suspend fun pushPending() {
        val pending = dao.pending()
        if (pending.isEmpty()) return
        val payload = JSONArray()
        pending.forEach { mutation ->
            payload.put(
                JSONObject()
                    .put("mutation_id", mutation.mutationId)
                    .put("entity", mutation.entityType)
                    .put("id", mutation.entityId)
                    .put("operation", mutation.operation)
                    .put("base_revision", mutation.baseRevision)
                    .put("data", JSONObject(mutation.json))
            )
        }
        val results = client.push(payload).getJSONArray("results")
        for (index in 0 until results.length()) {
            val result = results.getJSONObject(index)
            val mutation = pending.first { it.mutationId == result.getString("mutation_id") }
            when (result.getString("status")) {
                "applied" -> dao.removeMutation(mutation.mutationId)
                "conflict" -> {
                    dao.putConflict(
                        SyncConflict(
                            mutation.mutationId,
                            mutation.entityType,
                            mutation.entityId,
                            mutation.json,
                            result.optJSONObject("server_data")?.toString(),
                            result.optInt("server_revision"),
                            mutation.operation,
                        )
                    )
                    dao.removeMutation(mutation.mutationId)
                }
            }
        }
    }

    private suspend fun applyPull(response: JSONObject) {
        val state = SyncState(
            cursor = response.getLong("cursor"),
            accountJson = response.getJSONObject("account").toString(),
            performanceSettingsJson = response.optJSONObject("performance_settings")?.toString() ?: "{}",
            lastSyncAt = System.currentTimeMillis(),
        )
        val supporting = flattenSupporting(response.getJSONObject("supporting_entities")) +
            flattenReferenceData(response.optJSONObject("reference_data") ?: JSONObject())
        if (response.optBoolean("full_snapshot")) {
            dao.replaceSnapshot(flattenEntities(response.getJSONObject("entities")), supporting, state)
            return
        }
        dao.clearSupporting()
        dao.putSupporting(supporting)
        val changes = response.getJSONArray("changes")
        for (index in 0 until changes.length()) {
            val change = changes.getJSONObject(index)
            val type = change.getString("entity")
            val id = change.getString("id")
            if (change.getString("operation") == "delete") dao.deleteRecord(type, id)
            else dao.putRecords(listOf(CachedRecord(type, id, change.getInt("revision"), change.getJSONObject("data").toString())))
        }
        dao.putState(state)
    }

    private suspend fun refreshAttachmentCache() {
        val records = dao.records("song_attachment")
        val existing = dao.cachedAttachments().associateBy { it.attachmentId }
        val activeIds = records.mapTo(mutableSetOf()) { it.entityId }

        existing.values.filter { it.attachmentId !in activeIds }.forEach { stale ->
            stale.localPath?.let { runCatching { File(it).delete() } }
            dao.deleteCachedAttachment(stale.attachmentId)
        }

        records.forEach { record ->
            val data = JSONObject(record.json)
            val fileRef = data.optString("file_ref")
            val current = existing[record.entityId]
            if (fileRef.isBlank()) {
                current?.localPath?.let { runCatching { File(it).delete() } }
                dao.putCachedAttachment(record.toManifest(data, status = "unavailable"))
                return@forEach
            }
            if (fileRef.startsWith("http://", true) || fileRef.startsWith("https://", true)) {
                current?.localPath?.let { runCatching { File(it).delete() } }
                dao.putCachedAttachment(record.toManifest(data, status = "remote_only"))
                return@forEach
            }
            val currentFile = current?.localPath?.let(::File)
            if (current?.status == "ready" && current.revision == record.revision && current.fileRef == fileRef &&
                currentFile?.isFile == true && current.sha256 != null && current.sha256 == sha256(currentFile)
            ) {
                return@forEach
            }

            val destination = File(attachmentDirectory(), record.entityId + attachmentExtension(fileRef))
            runCatching { client.downloadAttachment(data.getString("download_path"), destination) }
                .onSuccess { download ->
                    if (download.localFile != null) {
                        dao.putCachedAttachment(
                            record.toManifest(
                                data,
                                status = "ready",
                                localPath = download.localFile.absolutePath,
                                mimeType = download.mimeType ?: attachmentMime(fileRef),
                                sha256 = download.sha256,
                                byteCount = download.byteCount,
                                cachedAt = System.currentTimeMillis(),
                            )
                        )
                    } else {
                        dao.putCachedAttachment(record.toManifest(data, status = "remote_only"))
                    }
                }
                .onFailure { error ->
                    dao.putCachedAttachment(
                        record.toManifest(
                            data,
                            status = "failed",
                            localPath = current?.localPath?.takeIf { File(it).isFile },
                            mimeType = current?.mimeType,
                            sha256 = current?.sha256,
                            byteCount = current?.byteCount,
                            cachedAt = current?.cachedAt,
                            error = error.message ?: "Download failed.",
                        )
                    )
                }
        }
    }

    private suspend fun refreshImageCache() = withContext(Dispatchers.IO) {
        val account = JSONObject(dao.syncState()?.accountJson ?: "{}")
        val imageUrls = buildList {
            add(account.optString("studio_logo_url"))
            for (type in listOf("item", "kit")) {
                dao.supporting(type).forEach { add(JSONObject(it.json).optString("image_url")) }
            }
        }.filter(String::isNotBlank).distinct()
        imageUrls.forEach { cacheImageFile(context, it) }
    }

    private fun attachmentDirectory(): File = File(context.filesDir, "offline-attachments").also(File::mkdirs)

    private fun flattenEntities(groups: JSONObject): List<CachedRecord> = buildList {
        groups.keys().forEach { type ->
            val rows = groups.getJSONArray(type)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(CachedRecord(type, row.getString("id"), row.optInt("revision"), row.getJSONObject("data").toString()))
            }
        }
    }

    private fun flattenSupporting(groups: JSONObject): List<SupportingRecord> = buildList {
        groups.keys().forEach { type ->
            val rows = groups.getJSONArray(type)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(SupportingRecord(type, row.getString("id"), row.getJSONObject("data").toString()))
            }
        }
    }

    private fun flattenReferenceData(groups: JSONObject): List<SupportingRecord> = buildList {
        groups.keys().forEach { type ->
            val rows = groups.getJSONArray(type)
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(SupportingRecord(type, row.get("id").toString(), row.toString()))
            }
        }
    }
}

private fun CachedRecord.toManifest(
    data: JSONObject,
    status: String,
    localPath: String? = null,
    mimeType: String? = null,
    sha256: String? = null,
    byteCount: Long? = null,
    cachedAt: Long? = null,
    error: String? = null,
) = CachedAttachment(
    attachmentId = entityId,
    songId = data.optString("song_id"),
    revision = revision,
    fileRef = data.optString("file_ref"),
    displayName = data.optString("display_name").ifBlank { data.optString("attachment_type", "Chart") },
    attachmentType = data.optString("attachment_type", "chart"),
    localPath = localPath,
    mimeType = mimeType,
    sha256 = sha256,
    byteCount = byteCount,
    status = status,
    error = error,
    cachedAt = cachedAt,
)

internal fun attachmentExtension(fileRef: String): String {
    val suffix = fileRef.substringBefore('?').substringAfterLast('.', "").lowercase()
    return when (suffix) {
        "pdf", "png", "jpg", "jpeg", "webp" -> ".$suffix"
        else -> ".bin"
    }
}

internal fun attachmentMime(fileRef: String): String? = when (attachmentExtension(fileRef)) {
    ".pdf" -> "application/pdf"
    ".png" -> "image/png"
    ".jpg", ".jpeg" -> "image/jpeg"
    ".webp" -> "image/webp"
    else -> null
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
