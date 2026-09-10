package com.manoogianmedia.studiorack.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _syncHealth = MutableStateFlow(RepositorySyncHealth())
    private val notifications = StudioRackNotifications(context, dao)

    fun signedIn() = tokenStore.isSignedIn()
    fun records(type: String): Flow<List<CachedRecord>> = dao.observeRecords(type)
    fun supporting(type: String): Flow<List<SupportingRecord>> = dao.observeSupporting(type)
    fun cachedAttachments(): Flow<List<CachedAttachment>> = dao.observeCachedAttachments()
    fun syncState(): Flow<SyncState?> = dao.observeSyncState()
    fun pendingCount(): Flow<Int> = dao.observePendingCount()
    fun conflicts(): Flow<List<SyncConflict>> = dao.observeConflicts()
    fun notificationCount(): Flow<Int> = dao.observeUnreadNotificationCount()
    fun syncHealth(): StateFlow<RepositorySyncHealth> = _syncHealth

    fun createNotificationChannels() = notifications.createChannels()
    suspend fun reconcileNotifications() = notifications.reconcile()
    suspend fun notifyDueEvent(eventId: String) = notifications.notifyDueEvent(eventId)
    suspend fun markNotificationRead(sourceId: String) = notifications.markRead(sourceId)

    suspend fun reportOverview(): JSONObject = client.reportOverview()

    suspend fun runAiReport(question: String): JSONObject = client.runAiReport(question)

    suspend fun liveEventStatus(eventId: String): JSONObject = client.liveEventStatus(eventId)

    suspend fun liveShareStatus(grantId: String): JSONObject = client.liveShareStatus(grantId)

    suspend fun localLivePacket(eventId: String): JSONObject {
        val event = dao.record("studio_event", eventId) ?: error("The scheduled session is not available on this device.")
        val eventJson = JSONObject(event.json)
        val setListId = eventJson.optString("set_list_id")
        check(setListId.isNotBlank()) { "This session does not have a set list." }
        val records = mutableListOf(event)
        dao.record("set_list", setListId)?.let(records::add)
        val sections = dao.records("set_list_section").filter { JSONObject(it.json).optString("set_list_id") == setListId }
        val entries = dao.records("set_list_entry").filter { JSONObject(it.json).optString("set_list_id") == setListId }
        records += sections
        records += entries
        val songIds = entries.map { JSONObject(it.json).optString("song_id") }.filter(String::isNotBlank).toSet()
        val songs = dao.records("song").filter { it.entityId in songIds }
        records += songs
        records += dao.records("song_attachment").filter { JSONObject(it.json).optString("song_id") in songIds }
        eventJson.optString("venue_id").takeIf(String::isNotBlank)?.let { venueId -> dao.record("venue", venueId)?.let(records::add) }
        return JSONObject()
            .put("event_id", eventId)
            .put("set_list_id", setListId)
            .put("records", JSONArray().apply {
                records.forEach { record ->
                    put(JSONObject().put("entity", record.entityType).put("id", record.entityId).put("revision", record.revision).put("data", JSONObject(record.json)))
                }
            })
    }

    suspend fun applyLocalLivePacket(packet: JSONObject) {
        val rows = packet.optJSONArray("records") ?: JSONArray()
        val upserts = buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(CachedRecord(row.getString("entity"), row.getString("id"), row.optInt("revision"), row.getJSONObject("data").toString()))
            }
        }
        val setListId = packet.optString("set_list_id")
        val desiredChildren = upserts.filter { it.entityType in setOf("set_list_section", "set_list_entry") }.mapTo(mutableSetOf()) { it.entityType to it.entityId }
        val removed = if (setListId.isBlank()) emptyList() else {
            (dao.records("set_list_section") + dao.records("set_list_entry"))
                .filter { JSONObject(it.json).optString("set_list_id") == setListId && it.entityType to it.entityId !in desiredChildren }
                .map { RecordRef(it.entityType, it.entityId) }
        }
        dao.applyLocalLivePacket(upserts, removed)
    }

    suspend fun acceptLocalLiveSetList(payload: JSONObject) {
        val setListId = payload.getString("set_list_id")
        val setList = payload.getJSONObject("set_list")
        val sectionsJson = payload.getJSONArray("sections")
        val entriesJson = payload.getJSONArray("entries")
        val sections = buildList {
            for (index in 0 until sectionsJson.length()) {
                val row = sectionsJson.getJSONObject(index)
                add(row.getString("id") to row)
            }
        }
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                val row = entriesJson.getJSONObject(index)
                add(row.getString("id") to row)
            }
        }
        saveSetList(setListId, setList, sections, entries)
    }

    suspend fun shareLink(grantId: String): String = client.shareLink(grantId).getString("share_url")

    suspend fun emailShare(grantId: String) {
        client.emailShare(grantId)
        sync()
    }

    suspend fun revokeShare(grantId: String) {
        client.revokeShare(grantId)
        sync()
    }

    suspend fun updateShare(grantId: String, data: JSONObject) {
        client.updateShare(grantId, data)
        sync()
    }

    suspend fun exportData(kind: String, format: String, ids: List<String> = emptyList()): DataExport =
        LocalExchangeExporter(
            dao,
            context.getString(com.manoogianmedia.studiorack.R.string.app_name),
            context.getString(com.manoogianmedia.studiorack.R.string.publisher_name),
            context.getString(com.manoogianmedia.studiorack.R.string.export_file_prefix),
        ).export(kind, format, ids)

    suspend fun importData(kind: String, file: File, displayName: String, mimeType: String): JSONObject {
        val result = client.importData(kind, file, displayName, mimeType)
        sync()
        return result
    }

    suspend fun saveSong(songId: String, data: JSONObject, newAttachments: List<SongAttachmentInput>) {
        val currentSong = dao.record("song", songId)
        val records = mutableListOf(CachedRecord("song", songId, currentSong?.revision ?: 0, data.toString()))
        val mutations = mutableListOf(
            mutation("song", songId, "upsert", currentSong?.revision ?: 0, data.toString(), System.currentTimeMillis())
        )
        val cached = mutableListOf<CachedAttachment>()
        var sequence = System.currentTimeMillis() + 1
        val existingAttachmentCount = dao.records("song_attachment").count { JSONObject(it.json).optString("song_id") == songId }
        newAttachments.forEachIndexed { index, input ->
            val attachmentId = "att_${UUID.randomUUID().toString().replace("-", "")}"
            val extension = attachmentExtension(input.displayName)
            val destination = File(attachmentDirectory(), attachmentId + extension)
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(input.uri))?.use { source ->
                    destination.outputStream().use(source::copyTo)
                } ?: error("The selected attachment could not be opened.")
            }
            val attachment = JSONObject()
                .put("id", attachmentId)
                .put("song_id", songId)
                .put("attachment_type", input.attachmentType)
                .put("display_name", input.displayName.substringBeforeLast('.').ifBlank { input.attachmentType })
                .put("instrument_role", "")
                .put("file_ref", "pending-upload://$attachmentId")
                .put("is_gig_default", if (existingAttachmentCount == 0 && index == 0) 1 else 0)
                .put("include_in_print", 1)
                .put("position", existingAttachmentCount + index + 1)
            records += CachedRecord("song_attachment", attachmentId, 0, attachment.toString())
            mutations += mutation("song_attachment", attachmentId, "upsert", 0, attachment.toString(), sequence++)
            cached += CachedAttachment(
                attachmentId = attachmentId,
                songId = songId,
                revision = 0,
                fileRef = attachment.getString("file_ref"),
                displayName = attachment.getString("display_name"),
                attachmentType = input.attachmentType,
                localPath = destination.absolutePath,
                mimeType = input.mimeType.ifBlank { attachmentMime(input.displayName).orEmpty() },
                sha256 = sha256(destination),
                byteCount = destination.length(),
                status = "ready",
                cachedAt = System.currentTimeMillis(),
            )
        }
        dao.queueSongBundle(records, mutations, cached)
        syncNow()
    }

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

    suspend fun saveEvent(eventId: String, event: JSONObject, ensembleIds: Set<String>, contactIds: Set<String>) {
        val relationTypes = setOf("studio_event_ensemble", "studio_event_contact")
        val existingRelations = relationTypes.flatMap { dao.records(it) }
            .filter { JSONObject(it.json).optString("event_id") == eventId }
        val desired = buildList {
            add(Triple("studio_event", eventId, event))
            ensembleIds.forEach { ensembleId ->
                add(Triple("studio_event_ensemble", "$eventId|$ensembleId", JSONObject()
                    .put("event_id", eventId).put("ensemble_id", ensembleId).put("relationship_role", "performer")))
            }
            contactIds.forEach { contactId ->
                add(Triple("studio_event_contact", "$eventId|$contactId", JSONObject()
                    .put("event_id", eventId).put("contact_id", contactId).put("relationship_role", "participant")))
            }
        }
        val existingByKey = (existingRelations + listOfNotNull(dao.record("studio_event", eventId)))
            .associateBy { it.entityType to it.entityId }
        val desiredKeys = desired.mapTo(mutableSetOf()) { it.first to it.second }
        val removed = existingRelations.filter { it.entityType to it.entityId !in desiredKeys }
        var sequence = System.currentTimeMillis()
        dao.applyLocalBundle(
            upserts = desired.map { (type, id, json) -> CachedRecord(type, id, existingByKey[type to id]?.revision ?: 0, json.toString()) },
            deletes = removed.map { RecordRef(it.entityType, it.entityId) },
            mutations = desired.map { (type, id, json) -> mutation(type, id, "upsert", existingByKey[type to id]?.revision ?: 0, json.toString(), sequence++) } +
                removed.map { mutation(it.entityType, it.entityId, "delete", it.revision, it.json, sequence++) },
        )
        notifications.reconcile()
        syncNow()
    }

    suspend fun saveContactRelationships(parentType: String, parentId: String, contactIds: Set<String>) {
        val relationType = if (parentType == "venue") "venue_contact" else "ensemble_contact"
        val parentKey = if (parentType == "venue") "venue_id" else "ensemble_id"
        val existing = dao.records(relationType).filter { JSONObject(it.json).optString(parentKey) == parentId }
        val existingById = existing.associateBy { it.entityId }
        val desired = contactIds.map { contactId ->
            val id = "$parentId|$contactId"
            id to JSONObject().put(parentKey, parentId).put("contact_id", contactId)
                .put("relationship_role", "").put("is_primary", 0).put("notes", "")
        }
        val desiredIds = desired.mapTo(mutableSetOf()) { it.first }
        val removed = existing.filter { it.entityId !in desiredIds }
        var sequence = System.currentTimeMillis()
        dao.applyLocalBundle(
            upserts = desired.map { (id, json) -> CachedRecord(relationType, id, existingById[id]?.revision ?: 0, json.toString()) },
            deletes = removed.map { RecordRef(it.entityType, it.entityId) },
            mutations = desired.map { (id, json) -> mutation(relationType, id, "upsert", existingById[id]?.revision ?: 0, json.toString(), sequence++) } +
                removed.map { mutation(it.entityType, it.entityId, "delete", it.revision, it.json, sequence++) },
        )
        syncNow()
    }

    suspend fun saveContact(contactId: String, data: JSONObject, methods: List<JSONObject>) {
        val currentContact = dao.record("contact", contactId)
        val existingMethods = dao.records("contact_method").filter { JSONObject(it.json).optString("contact_id") == contactId }
        val existingById = existingMethods.associateBy { it.entityId }
        val desired = methods.mapIndexedNotNull { index, source ->
            val value = source.optString("value").trim()
            if (value.isBlank()) return@mapIndexedNotNull null
            val id = source.optString("id").ifBlank { "cm_${UUID.randomUUID().toString().replace("-", "")}" }
            id to JSONObject(source.toString()).put("id", id).put("contact_id", contactId).put("position", index)
        }
        val desiredIds = desired.mapTo(mutableSetOf()) { it.first }
        val removed = existingMethods.filter { it.entityId !in desiredIds }
        var sequence = System.currentTimeMillis()
        val root = CachedRecord("contact", contactId, currentContact?.revision ?: 0, data.toString())
        dao.applyLocalBundle(
            upserts = listOf(root) + desired.map { (id, json) -> CachedRecord("contact_method", id, existingById[id]?.revision ?: 0, json.toString()) },
            deletes = removed.map { RecordRef(it.entityType, it.entityId) },
            mutations = listOf(mutation("contact", contactId, "upsert", currentContact?.revision ?: 0, data.toString(), sequence++)) +
                desired.map { (id, json) -> mutation("contact_method", id, "upsert", existingById[id]?.revision ?: 0, json.toString(), sequence++) } +
                removed.map { mutation(it.entityType, it.entityId, "delete", it.revision, it.json, sequence++) },
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
        dao.applyLocalBundle(
            listOf(CachedRecord(entityType, entityId, revision, data.toString())), emptyList(), listOf(
            PendingMutation(
                mutationId = "mutation_${UUID.randomUUID().toString().replace("-", "")}",
                entityType = entityType,
                entityId = entityId,
                operation = "upsert",
                baseRevision = revision,
                json = data.toString(),
            ))
        )
        if (entityType == "studio_event" || entityType == "maintenance_note" || entityType == "maintenance_record") {
            notifications.reconcile()
        }
        syncNow()
    }

    suspend fun uploadDirectoryImage(uri: String, displayName: String, mimeType: String): String {
        val extension = attachmentExtension(displayName).ifBlank { ".jpg" }
        val temporary = File.createTempFile("venue-photo-", extension, context.cacheDir)
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { source ->
                    temporary.outputStream().use(source::copyTo)
                } ?: error("The selected directory photo could not be opened.")
            }
            return client.uploadAttachment(temporary, displayName, mimeType).getString("file_ref")
        } finally {
            temporary.delete()
        }
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
        notifications.clearAll()
        tokenStore.clear()
    }

    suspend fun sync() {
        _syncHealth.value = _syncHealth.value.copy(running = true, error = null)
        try {
            pushPendingAttachments()
            pushPending()
            var cursor = dao.syncState()?.cursor ?: 0
            do {
                val response = client.pull(cursor)
                applyPull(response)
                cursor = response.getLong("cursor")
            } while (response.optBoolean("has_more", false))
            refreshAttachmentCache()
            refreshImageCache()
            notifications.reconcile()
            _syncHealth.value = RepositorySyncHealth(running = false, lastSuccessAt = System.currentTimeMillis())
        } catch (error: Exception) {
            _syncHealth.value = _syncHealth.value.copy(running = false, error = error.message ?: "Synchronization failed.")
            throw error
        }
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

    private suspend fun pushPendingAttachments() {
        val pending = dao.records("song_attachment").filter { JSONObject(it.json).optString("file_ref").startsWith("pending-upload://") }
        val cachedById = dao.cachedAttachments().associateBy(CachedAttachment::attachmentId)
        pending.forEach { record ->
            val data = JSONObject(record.json)
            val cached = cachedById[record.entityId] ?: error("The local attachment file is unavailable.")
            val localFile = cached.localPath?.let(::File)?.takeIf(File::isFile)
                ?: error("The local attachment file is unavailable.")
            val uploaded = client.uploadAttachment(localFile, localFile.name, cached.mimeType.orEmpty())
            data.put("file_ref", uploaded.getString("file_ref"))
            data.put("download_path", "/api/v1/attachments/${record.entityId}")
            dao.putRecords(listOf(record.copy(json = data.toString())))
            dao.removePendingForEntity("song_attachment", record.entityId)
            dao.putPending(mutation("song_attachment", record.entityId, "upsert", record.revision, data.toString(), System.currentTimeMillis()))
            dao.putCachedAttachment(cached.copy(fileRef = data.getString("file_ref"), status = "ready", error = null))
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
        val records = dao.records("song_attachment") + dao.supporting("shared_song_attachment").map { shared ->
            val data = JSONObject(shared.json)
            CachedRecord("shared_song_attachment", shared.entityId, data.optString("updated_utc").hashCode(), shared.json)
        }
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
            for (type in listOf("venue", "contact", "ensemble")) {
                dao.records(type).forEach { add(JSONObject(it.json).optString("image_url")) }
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
        "pdf", "png", "jpg", "jpeg", "gif", "webp", "txt", "doc", "docx" -> ".$suffix"
        else -> ".bin"
    }
}

internal fun attachmentMime(fileRef: String): String? = when (attachmentExtension(fileRef)) {
    ".pdf" -> "application/pdf"
    ".png" -> "image/png"
    ".jpg", ".jpeg" -> "image/jpeg"
    ".webp" -> "image/webp"
    ".gif" -> "image/gif"
    ".txt" -> "text/plain"
    ".doc" -> "application/msword"
    ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    else -> null
}

data class SongAttachmentInput(
    val uri: String,
    val displayName: String,
    val attachmentType: String,
    val mimeType: String,
)

data class RepositorySyncHealth(
    val running: Boolean = false,
    val error: String? = null,
    val lastSuccessAt: Long? = null,
)

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
