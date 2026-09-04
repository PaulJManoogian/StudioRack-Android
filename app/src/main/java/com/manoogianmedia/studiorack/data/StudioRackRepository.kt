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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class StudioRackRepository(
    private val context: Context,
    private val dao: StudioRackDao,
    private val tokenStore: TokenStore,
    private val client: SyncClient,
) {
    fun signedIn() = tokenStore.isSignedIn()
    fun records(type: String): Flow<List<CachedRecord>> = dao.observeRecords(type)
    fun cachedAttachments(): Flow<List<CachedAttachment>> = dao.observeCachedAttachments()
    fun syncState(): Flow<SyncState?> = dao.observeSyncState()

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
        val supporting = flattenSupporting(response.getJSONObject("supporting_entities"))
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
