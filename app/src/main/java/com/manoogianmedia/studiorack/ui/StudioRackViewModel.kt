package com.manoogianmedia.studiorack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.CachedAttachment
import com.manoogianmedia.studiorack.data.StudioRackRepository
import com.manoogianmedia.studiorack.data.SongAttachmentInput
import com.manoogianmedia.studiorack.data.SyncState
import com.manoogianmedia.studiorack.data.SupportingRecord
import com.manoogianmedia.studiorack.data.SyncConflict
import com.manoogianmedia.studiorack.data.RepositorySyncHealth
import com.manoogianmedia.studiorack.data.DataExport
import com.manoogianmedia.studiorack.data.LocalLiveCoordinator
import com.manoogianmedia.studiorack.data.LocalLivePeer
import com.manoogianmedia.studiorack.data.LocalLiveRole
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.UUID

class StudioRackViewModel(
    private val repository: StudioRackRepository,
    private val localLiveCoordinator: LocalLiveCoordinator,
) : ViewModel() {
    private val setListSaveMutex = Mutex()
    val events: StateFlow<List<CachedRecord>> = repository.records("studio_event")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val songs: StateFlow<List<CachedRecord>> = repository.records("song")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val setLists: StateFlow<List<CachedRecord>> = repository.records("set_list")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sections: StateFlow<List<CachedRecord>> = repository.records("set_list_section")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val entries: StateFlow<List<CachedRecord>> = repository.records("set_list_entry")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val attachments: StateFlow<List<CachedRecord>> = repository.records("song_attachment")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val maintenanceNotes: StateFlow<List<CachedRecord>> = repository.records("maintenance_note")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val maintenanceHistory: StateFlow<List<CachedRecord>> = repository.records("maintenance_record")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val venues: StateFlow<List<CachedRecord>> = repository.records("venue")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contacts: StateFlow<List<CachedRecord>> = repository.records("contact")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contactMethods: StateFlow<List<CachedRecord>> = repository.records("contact_method")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ensembles: StateFlow<List<CachedRecord>> = repository.records("ensemble")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val venueContacts: StateFlow<List<CachedRecord>> = repository.records("venue_contact")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ensembleContacts: StateFlow<List<CachedRecord>> = repository.records("ensemble_contact")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val eventEnsembles: StateFlow<List<CachedRecord>> = repository.records("studio_event_ensemble")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val eventContacts: StateFlow<List<CachedRecord>> = repository.records("studio_event_contact")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cachedAttachments: StateFlow<List<CachedAttachment>> = repository.cachedAttachments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val items: StateFlow<List<SupportingRecord>> = repository.supporting("item")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val kits: StateFlow<List<SupportingRecord>> = repository.supporting("kit")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val itemSpecs: StateFlow<List<SupportingRecord>> = repository.supporting("item_spec")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val itemUnits: StateFlow<List<SupportingRecord>> = repository.supporting("item_unit")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val kitMembers: StateFlow<List<SupportingRecord>> = repository.supporting("kit_member")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val kitMemberUnits: StateFlow<List<SupportingRecord>> = repository.supporting("kit_member_unit")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val buddyActions: StateFlow<List<SupportingRecord>> = repository.supporting("studio_buddy_action")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val buddySkills: StateFlow<List<SupportingRecord>> = repository.supporting("studio_buddy_skill")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reportRuns: StateFlow<List<SupportingRecord>> = repository.supporting("report_run")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories: StateFlow<List<SupportingRecord>> = repository.supporting("category")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val itemTypes: StateFlow<List<SupportingRecord>> = repository.supporting("item_type")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val brands: StateFlow<List<SupportingRecord>> = repository.supporting("brand")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val locations: StateFlow<List<SupportingRecord>> = repository.supporting("location")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val statuses: StateFlow<List<SupportingRecord>> = repository.supporting("status_option")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val ownedShares: StateFlow<List<SupportingRecord>> = repository.supporting("owned_share")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedAccess: StateFlow<List<SupportingRecord>> = repository.supporting("shared_access")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedEvents: StateFlow<List<SupportingRecord>> = repository.supporting("shared_event")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedVenues: StateFlow<List<SupportingRecord>> = repository.supporting("shared_venue")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedSetLists: StateFlow<List<SupportingRecord>> = repository.supporting("shared_set_list")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedSetListSections: StateFlow<List<SupportingRecord>> = repository.supporting("shared_set_list_section")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedSetListEntries: StateFlow<List<SupportingRecord>> = repository.supporting("shared_set_list_entry")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedSongs: StateFlow<List<SupportingRecord>> = repository.supporting("shared_song")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sharedAttachments: StateFlow<List<SupportingRecord>> = repository.supporting("shared_song_attachment")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val syncState: StateFlow<SyncState?> = repository.syncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pendingCount: StateFlow<Int> = repository.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val conflicts: StateFlow<List<SyncConflict>> = repository.conflicts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notificationCount: StateFlow<Int> = repository.notificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val syncHealth: StateFlow<RepositorySyncHealth> = repository.syncHealth()
    val localLive = localLiveCoordinator.state
    val nearbyLiveSessions = localLiveCoordinator.peers

    private val initiallySignedIn = repository.signedIn()
    private val _uiState = MutableStateFlow(StudioRackUiState(initiallySignedIn, starting = initiallySignedIn))
    val uiState: StateFlow<StudioRackUiState> = _uiState.asStateFlow()
    private val _reportState = MutableStateFlow(ReportUiState())
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    init {
        if (initiallySignedIn) {
            viewModelScope.launch {
                val startedAt = System.currentTimeMillis()
                val result = runCatching { repository.sync() }
                delay((800L - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0L))
                result
                    .onSuccess { _uiState.value = _uiState.value.copy(starting = false, message = "Synced.", syncError = false) }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(
                            starting = false,
                            message = "Offline: showing the last synchronized data.",
                            syncError = true,
                        )
                    }
                runCatching { repository.reconcileNotifications() }
            }
        }
    }

    fun signIn(email: String, accessCode: String, mfaCode: String) {
        _uiState.value = _uiState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.signIn(email, accessCode, mfaCode) }
                .onSuccess { _uiState.value = StudioRackUiState(signedIn = true, message = "This device is ready offline.", syncError = false) }
                .onFailure { _uiState.value = StudioRackUiState(signedIn = false, message = it.message ?: "Sign-in failed.") }
        }
    }

    fun sync() {
        _uiState.value = _uiState.value.copy(busy = true, syncError = false)
        viewModelScope.launch {
            runCatching { repository.sync() }
                .onSuccess { _uiState.value = _uiState.value.copy(busy = false, message = "Synced.", syncError = false) }
                .onFailure { _uiState.value = _uiState.value.copy(busy = false, message = it.message ?: "Synchronization failed.", syncError = true) }
        }
    }

    suspend fun refreshLiveEvent(eventId: String, knownRevision: String): LiveRefreshResult = runCatching {
        val status = repository.liveEventStatus(eventId)
        val revision = status.optString("revision")
        if (knownRevision.isBlank() || (revision.isNotBlank() && revision != knownRevision)) repository.sync()
        LiveRefreshResult(revision, connected = true, changed = knownRevision.isNotBlank() && revision != knownRevision)
    }.getOrElse { LiveRefreshResult(knownRevision, connected = false, changed = false) }

    suspend fun refreshLiveShare(grantId: String, knownRevision: String): LiveRefreshResult = runCatching {
        val status = repository.liveShareStatus(grantId)
        val revision = status.optString("revision")
        if (knownRevision.isBlank() || (revision.isNotBlank() && revision != knownRevision)) repository.sync()
        LiveRefreshResult(revision, connected = true, changed = knownRevision.isNotBlank() && revision != knownRevision)
    }.getOrElse { LiveRefreshResult(knownRevision, connected = false, changed = false) }

    fun refreshNotifications() {
        viewModelScope.launch { runCatching { repository.reconcileNotifications() } }
    }

    fun hostLocalLive(eventId: String, sessionName: String) {
        viewModelScope.launch {
            runCatching {
                localLiveCoordinator.host(
                    eventId,
                    sessionName,
                    providePacket = { repository.localLivePacket(eventId) },
                    acceptSetList = { repository.acceptLocalLiveSetList(it) },
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = "Local live session is hosting on this Wi-Fi.", syncError = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = it.message ?: "Could not host the local live session.", syncError = true)
            }
        }
    }

    fun joinLocalLive(peer: LocalLivePeer, code: String) {
        if (code.length != 6) {
            _uiState.value = _uiState.value.copy(message = "Enter the host's six-digit session code.", syncError = true)
            return
        }
        localLiveCoordinator.join(peer, code) { repository.applyLocalLivePacket(it) }
        _uiState.value = _uiState.value.copy(message = "Connecting to ${peer.name} on local Wi-Fi.", syncError = false)
    }

    fun leaveLocalLive() {
        localLiveCoordinator.stop()
        _uiState.value = _uiState.value.copy(message = "Local live session closed.", syncError = false)
    }

    fun copyShareLink(grantId: String, done: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.shareLink(grantId) }
                .onSuccess { link -> _uiState.value = _uiState.value.copy(message = "Guest link copied."); done(link) }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not retrieve the guest link.", syncError = true); done(null) }
        }
    }

    fun emailShare(grantId: String) {
        viewModelScope.launch {
            runCatching { repository.emailShare(grantId) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Guest link emailed.", syncError = false) }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not email the guest link.", syncError = true) }
        }
    }

    fun updateShare(grantId: String, data: JSONObject, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.updateShare(grantId, data) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Shared access updated.", syncError = false); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not update shared access.", syncError = true) }
        }
    }

    fun revokeShare(grantId: String, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.revokeShare(grantId) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Shared access revoked.", syncError = false); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not revoke shared access.", syncError = true) }
        }
    }

    fun saveSong(id: String?, data: JSONObject, attachments: List<SongAttachmentInput>, done: () -> Unit) {
        val songId = id ?: "song_${UUID.randomUUID().toString().replace("-", "")}"
        viewModelScope.launch {
            runCatching { repository.saveSong(songId, data, attachments) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(message = "Song and attachments saved offline. Sync is queued.")
                    done()
                }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save the song.") }
        }
    }

    fun deleteSong(id: String, done: () -> Unit) = deleteRecord("song", id, done)

    fun saveEvent(id: String?, data: JSONObject, ensembleIds: Set<String>, contactIds: Set<String>, done: () -> Unit) {
        val eventId = id ?: "event_${UUID.randomUUID().toString().replace("-", "")}"
        viewModelScope.launch {
            runCatching { repository.saveEvent(eventId, data, ensembleIds, contactIds) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Scheduled item saved. Synchronization is queued."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save the scheduled item.") }
        }
    }

    fun deleteEvent(id: String, done: () -> Unit) = deleteRecord("studio_event", id, done)

    fun saveDirectoryRecord(entityType: String, id: String?, data: JSONObject, imageUri: String? = null, imageName: String = "", imageMimeType: String = "", contactMethods: List<JSONObject> = emptyList(), done: () -> Unit) {
        val prefix = when (entityType) { "venue" -> "venue"; "contact" -> "contact"; else -> "ensemble" }
        val recordId = id ?: "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
        viewModelScope.launch {
            runCatching {
                if (entityType in setOf("venue", "contact") && !imageUri.isNullOrBlank()) {
                    data.put("image_url", repository.uploadDirectoryImage(imageUri, imageName, imageMimeType))
                }
                if (entityType == "contact") repository.saveContact(recordId, data, contactMethods)
                else repository.save(entityType, recordId, data)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = "${prefix.replaceFirstChar(Char::uppercase)} saved. Synchronization is queued.")
                done()
            }.onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save the record.") }
        }
    }

    fun deleteDirectoryRecord(entityType: String, id: String, done: () -> Unit) = deleteRecord(entityType, id, done)

    fun saveDirectoryRelationships(parentType: String, parentId: String, contactIds: Set<String>, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.saveContactRelationships(parentType, parentId, contactIds) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Contact relationships saved. Synchronization is queued."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save contact relationships.") }
        }
    }

    fun saveMaintenanceNote(itemId: String, note: String, done: () -> Unit) {
        val id = "maintenance_${UUID.randomUUID().toString().replace("-", "")}"
        saveRecord(
            "maintenance_note",
            id,
            JSONObject().put("item_id", itemId).put("note", note.trim()).put("status", "pending").put("source", "android"),
            done,
        )
    }

    fun completeMaintenance(data: JSONObject, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.save("maintenance_record", "service_${UUID.randomUUID().toString().replace("-", "")}", data) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Service recorded. Synchronization is queued."); done(true) }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not record service."); done(false) }
        }
    }

    fun renameSetList(record: CachedRecord, name: String, done: () -> Unit) = saveRecord(
        "set_list", record.entityId, JSONObject(record.json).put("name", name.trim()), done,
    )

    fun saveSetList(draft: SetListDraft, done: () -> Unit, liveAutosave: Boolean = false, failed: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                setListSaveMutex.withLock {
                    val payload = setListPayload(draft)
                    val localState = localLiveCoordinator.state.value
                    if (liveAutosave && localState.role == LocalLiveRole.GUEST && localState.setListId == draft.id) {
                        repository.applyLocalLivePacket(localLiveCoordinator.submitSetList(payload))
                    } else {
                        repository.acceptLocalLiveSetList(payload)
                        if (liveAutosave && localState.role == LocalLiveRole.HOST && localState.setListId == draft.id) localLiveCoordinator.hostChanged()
                    }
                }
            }.onSuccess {
                if (!liveAutosave) _uiState.value = _uiState.value.copy(message = "Set list saved offline. Sync is queued.")
                done()
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save set list.")
                failed()
            }
        }
    }

    private fun setListPayload(draft: SetListDraft): JSONObject {
        val setList = JSONObject().put("id", draft.id).put("name", draft.name.trim())
            .put("description", draft.description.trim()).put("notes", draft.notes.trim())
            .put("print_charts", if (draft.attachmentPrintMode == "none") 0 else 1)
            .put("attachment_print_mode", draft.attachmentPrintMode).put("is_favorite", if (draft.favorite) 1 else 0)
        val sections = JSONArray()
        val entries = JSONArray()
        draft.sections.forEachIndexed { index, section ->
            sections.put(JSONObject().put("id", section.id).put("set_list_id", draft.id)
                .put("name", section.name.trim().ifBlank { "Set ${index + 1}" }).put("position", index).put("notes", section.notes.trim()))
            section.entries.forEachIndexed { entryIndex, entry ->
                entries.put(JSONObject().put("id", entry.id).put("set_list_id", draft.id)
                    .put("section_id", section.id).put("song_id", entry.songId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                    .put("position", entryIndex).put("manual_title", entry.manualTitle.trim())
                    .put("entry_notes", entry.notes.trim())
                    .put("performance_attachment_id", entry.performanceAttachmentId?.takeIf(String::isNotBlank) ?: JSONObject.NULL))
            }
        }
        return JSONObject().put("set_list_id", draft.id).put("set_list", setList).put("sections", sections).put("entries", entries)
    }

    fun deleteSetList(id: String, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.deleteSetList(id) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Set list deletion queued for sync."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not delete set list.") }
        }
    }

    fun resolveConflict(conflict: SyncConflict, keepLocal: Boolean) {
        viewModelScope.launch { repository.resolveConflict(conflict, keepLocal) }
    }

    fun refreshReportOverview() {
        _reportState.value = _reportState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.reportOverview() }
                .onSuccess { _reportState.value = _reportState.value.copy(busy = false, onlineOverview = it, message = "Server report refreshed.") }
                .onFailure { _reportState.value = _reportState.value.copy(busy = false, message = it.message ?: "Server reporting is unavailable while offline.") }
        }
    }

    fun runAiReport(question: String) {
        _reportState.value = _reportState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.runAiReport(question.trim()) }
                .onSuccess { result ->
                    _reportState.value = _reportState.value.copy(busy = false, aiResult = result, message = "Report complete.")
                    runCatching { repository.sync() }
                }
                .onFailure { _reportState.value = _reportState.value.copy(busy = false, message = it.message ?: "AI reporting is unavailable while offline.") }
        }
    }

    fun exportData(kind: String, format: String, ids: List<String> = emptyList(), done: (DataExport?) -> Unit) {
        _reportState.value = _reportState.value.copy(busy = true, message = "Preparing export...")
        viewModelScope.launch {
            runCatching { repository.exportData(kind, format, ids) }
                .onSuccess {
                    _reportState.value = _reportState.value.copy(busy = false, message = "Export ready.")
                    done(it)
                }
                .onFailure {
                    _reportState.value = _reportState.value.copy(busy = false, message = it.message ?: "Data export failed.")
                    done(null)
                }
        }
    }

    fun importData(kind: String, file: File, displayName: String, mimeType: String, done: () -> Unit) {
        _reportState.value = _reportState.value.copy(busy = true, message = "Importing data...")
        viewModelScope.launch {
            val outcome = runCatching { repository.importData(kind, file, displayName, mimeType) }
            outcome
                .onSuccess { result ->
                    _reportState.value = _reportState.value.copy(
                        busy = false,
                        message = "Import complete: ${result.optInt("created")} created, ${result.optInt("updated")} updated, ${result.optInt("skipped")} skipped.",
                    )
                    done()
                }
                .onFailure { _reportState.value = _reportState.value.copy(busy = false, message = it.message ?: "Data import failed.") }
            done()
        }
    }

    private fun saveRecord(type: String, id: String, data: JSONObject, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.save(type, id, data) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Saved offline. Sync is queued."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save.") }
        }
    }

    private fun deleteRecord(type: String, id: String, done: () -> Unit) {
        viewModelScope.launch {
            runCatching { repository.delete(type, id) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Deletion queued for sync."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not delete.") }
        }
    }
}

data class StudioRackUiState(
    val signedIn: Boolean,
    val busy: Boolean = false,
    val message: String = "",
    val starting: Boolean = false,
    val syncError: Boolean = false,
)

data class ReportUiState(
    val busy: Boolean = false,
    val message: String = "",
    val onlineOverview: JSONObject? = null,
    val aiResult: JSONObject? = null,
)

data class LiveRefreshResult(val revision: String, val connected: Boolean, val changed: Boolean)

data class SetListDraft(
    val id: String,
    val name: String = "",
    val description: String = "",
    val notes: String = "",
    val attachmentPrintMode: String = "none",
    val favorite: Boolean = false,
    val sections: List<SetSectionDraft> = emptyList(),
)

data class SetSectionDraft(val id: String, val name: String = "", val notes: String = "", val entries: List<SetEntryDraft> = emptyList())
data class SetEntryDraft(
    val id: String,
    val songId: String? = null,
    val manualTitle: String = "",
    val notes: String = "",
    val performanceAttachmentId: String? = null,
)

class StudioRackViewModelFactory(
    private val repository: StudioRackRepository,
    private val localLiveCoordinator: LocalLiveCoordinator,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StudioRackViewModel(repository, localLiveCoordinator) as T
}
