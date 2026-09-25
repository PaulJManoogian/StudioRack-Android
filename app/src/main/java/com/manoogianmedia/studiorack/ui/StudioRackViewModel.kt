package com.manoogianmedia.studiorack.ui

import android.app.Activity
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
import com.manoogianmedia.studiorack.performance.PerformanceSettings
import com.manoogianmedia.studiorack.crew.CrewBehaviorSettings
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
    val performanceCues: StateFlow<List<CachedRecord>> = repository.records("performance_cue")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveAudioBuses: StateFlow<List<CachedRecord>> = repository.records("live_audio_bus")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveAudioArrangements: StateFlow<List<CachedRecord>> = repository.records("live_audio_arrangement")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveAudioRouteProfiles: StateFlow<List<CachedRecord>> = repository.records("live_audio_route_profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liveAudioRoutes: StateFlow<List<CachedRecord>> = repository.records("live_audio_route")
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
    val eventKits: StateFlow<List<CachedRecord>> = repository.records("studio_event_kit")
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
    val workspaceQueries: StateFlow<List<SupportingRecord>> = repository.supporting("crew_workspace_query")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val buddySkills: StateFlow<List<SupportingRecord>> = repository.supporting("studio_buddy_skill")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reportRuns: StateFlow<List<SupportingRecord>> = repository.supporting("report_run")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories: StateFlow<List<SupportingRecord>> = repository.supporting("category")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val itemTypes: StateFlow<List<SupportingRecord>> = repository.supporting("item_type")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workspaceModules: StateFlow<List<SupportingRecord>> = repository.supporting("workspace_module")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val workspaceProfiles: StateFlow<List<SupportingRecord>> = repository.supporting("workspace_profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val brands: StateFlow<List<SupportingRecord>> = repository.supporting("brand")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val locations: StateFlow<List<SupportingRecord>> = repository.supporting("location")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val statuses: StateFlow<List<SupportingRecord>> = repository.supporting("status_option")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val kitDesignations: StateFlow<List<SupportingRecord>> = repository.supporting("kit_designation")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val drumHeads: StateFlow<List<SupportingRecord>> = repository.supporting("drum_head_option")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dampeningOptions: StateFlow<List<SupportingRecord>> = repository.supporting("dampening_option")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val assetStyles: StateFlow<List<SupportingRecord>> = repository.supporting("asset_style_option")
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
    val backgroundSyncWifiOnly: StateFlow<Boolean> = repository.backgroundSyncWifiOnly()
    val localLive = localLiveCoordinator.state
    val nearbyLiveSessions = localLiveCoordinator.peers

    private val initiallySignedIn = repository.signedIn()
    private val _uiState = MutableStateFlow(StudioRackUiState(initiallySignedIn, starting = initiallySignedIn))
    val uiState: StateFlow<StudioRackUiState> = _uiState.asStateFlow()
    private val _reportState = MutableStateFlow(ReportUiState())
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()
    private val _workspaceMembersState = MutableStateFlow(WorkspaceMembersUiState())
    val workspaceMembersState: StateFlow<WorkspaceMembersUiState> = _workspaceMembersState.asStateFlow()

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

    fun signInWithPasskey(activity: Activity) {
        _uiState.value = _uiState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.signInWithPasskey(activity) }
                .onSuccess { _uiState.value = StudioRackUiState(signedIn = true, message = "Signed in with your passkey.", syncError = false) }
                .onFailure { _uiState.value = StudioRackUiState(signedIn = false, message = it.passkeyMessage("Passkey sign-in failed."), syncError = true) }
        }
    }

    fun signOut() {
        _uiState.value = _uiState.value.copy(busy = true, message = "Signing out...")
        viewModelScope.launch {
            runCatching { repository.signOut() }
                .onSuccess { _uiState.value = StudioRackUiState(signedIn = false, message = "Signed out.") }
                .onFailure { _uiState.value = _uiState.value.copy(busy = false, message = it.message ?: "Could not sign out.", syncError = true) }
        }
    }

    fun createPasskey(activity: Activity) {
        _uiState.value = _uiState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.createPasskey(activity) }
                .onSuccess { _uiState.value = _uiState.value.copy(busy = false, message = "Passkey added to this Studio Leviathan login.", syncError = false) }
                .onFailure { _uiState.value = _uiState.value.copy(busy = false, message = it.passkeyMessage("Passkey could not be created."), syncError = true) }
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

    fun savePerformanceSettings(settings: PerformanceSettings) {
        viewModelScope.launch {
            runCatching { repository.savePerformanceSettings(settings.toJson(pendingSync = true)) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = "Leviathan Live settings saved. They will synchronize when connected.",
                        syncError = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        message = it.message ?: "Could not save Leviathan Live settings.",
                        syncError = true,
                    )
                }
        }
    }

    fun saveCrewBehaviorSettings(settings: CrewBehaviorSettings) {
        viewModelScope.launch {
            runCatching { repository.saveCrewBehaviorSettings(settings.toJson(pendingSync = true)) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = "Crew behavior saved. It will synchronize when connected.",
                        syncError = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        message = it.message ?: "Could not save Crew behavior.",
                        syncError = true,
                    )
                }
        }
    }

    fun updateWorkspaceProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = "Updating workspace modules...")
            runCatching { repository.updateWorkspaceProfile(profileId) }
                .onSuccess { _uiState.value = _uiState.value.copy(busy = false, message = "Workspace focus updated.", syncError = false) }
                .onFailure { _uiState.value = _uiState.value.copy(busy = false, message = it.message ?: "Workspace modules could not be updated.", syncError = true) }
        }
    }

    fun setBackgroundSyncWifiOnly(enabled: Boolean) {
        repository.setBackgroundSyncWifiOnly(enabled)
        _uiState.value = _uiState.value.copy(
            message = if (enabled) {
                "Background notifications will synchronize on Wi-Fi only."
            } else {
                "Background notifications may synchronize over Wi-Fi or mobile data."
            },
            syncError = false,
        )
    }

    suspend fun searchSongMetadata(title: String, artist: String): JSONObject =
        repository.searchSongMetadata(title, artist)

    suspend fun searchSongLyrics(title: String, artist: String, album: String): JSONObject =
        repository.searchSongLyrics(title, artist, album)

    suspend fun structureSongLyrics(title: String, artist: String, album: String, lyrics: String): JSONObject =
        repository.structureSongLyrics(title, artist, album, lyrics)

    suspend fun fillMissingSongLengths(): JSONObject = repository.fillMissingSongLengths()

    suspend fun applySongDuration(songId: String, candidate: JSONObject): JSONObject =
        repository.applySongDuration(songId, candidate)

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

    fun loadWorkspaceMembers() {
        _workspaceMembersState.value = _workspaceMembersState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.workspaceMembers() }
                .onSuccess { _workspaceMembersState.value = WorkspaceMembersUiState(payload = it) }
                .onFailure { _workspaceMembersState.value = WorkspaceMembersUiState(message = it.message ?: "Workspace members require a connection.", error = true) }
        }
    }

    fun inviteWorkspaceMember(name: String, email: String, role: String, contactId: String, done: () -> Unit) {
        _workspaceMembersState.value = _workspaceMembersState.value.copy(busy = true, message = "Sending invitation...")
        viewModelScope.launch {
            runCatching {
                repository.inviteWorkspaceMember(JSONObject().put("name", name.trim()).put("email", email.trim()).put("role", role).put("contact_id", contactId))
            }.onSuccess {
                _workspaceMembersState.value = _workspaceMembersState.value.copy(message = it.optString("message", "Invitation sent."), error = false)
                loadWorkspaceMembers()
                done()
            }.onFailure { _workspaceMembersState.value = _workspaceMembersState.value.copy(busy = false, message = it.message ?: "Could not send the invitation.", error = true) }
        }
    }

    fun workspaceMemberAction(memberId: String, action: String, role: String = "") {
        _workspaceMembersState.value = _workspaceMembersState.value.copy(busy = true, message = "Updating member...")
        viewModelScope.launch {
            runCatching { repository.workspaceMemberAction(memberId, action, role) }
                .onSuccess { loadWorkspaceMembers() }
                .onFailure { _workspaceMembersState.value = _workspaceMembersState.value.copy(busy = false, message = it.message ?: "Could not update the member.", error = true) }
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

    fun saveSongAttachment(id: String, data: JSONObject) {
        viewModelScope.launch {
            runCatching { repository.saveSongAttachment(id, data) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Performance material saved. Synchronization is queued.", syncError = false) }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save performance material.", syncError = true) }
        }
    }

    fun deleteSongAttachment(id: String) = deleteRecord("song_attachment", id) {}

    fun saveLiveAudioBus(id: String?, data: JSONObject, done: () -> Unit = {}) {
        val recordId = id ?: "bus_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        viewModelScope.launch {
            runCatching { repository.save("live_audio_bus", recordId, data.put("id", recordId)) }
                .onSuccess { _uiState.value = _uiState.value.copy(message = "Audio bus saved. Synchronization is queued."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save the audio bus.", syncError = true) }
        }
    }

    fun saveLiveAudioProfile(id: String?, data: JSONObject, routeData: List<Pair<String?, JSONObject>>, done: () -> Unit = {}) {
        val profileId = id ?: "routeprof_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        viewModelScope.launch {
            runCatching {
                val records = buildList {
                    if (data.optInt("is_default") == 1) {
                        liveAudioRouteProfiles.value.filter { it.entityId != profileId }.forEach { profile ->
                            add(Triple("live_audio_route_profile", profile.entityId, JSONObject(profile.json).put("is_default", 0)))
                        }
                    }
                    add(Triple("live_audio_route_profile", profileId, data.put("id", profileId)))
                    routeData.forEach { (existingId, route) ->
                        val routeId = existingId ?: "route_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                        add(Triple("live_audio_route", routeId, route.put("id", routeId).put("profile_id", profileId)))
                    }
                }
                repository.saveBundle(records)
            }.onSuccess { _uiState.value = _uiState.value.copy(message = "Routing profile saved. Synchronization is queued."); done() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save the routing profile.", syncError = true) }
        }
    }

    fun deleteLiveAudioProfile(id: String, done: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val routes = liveAudioRoutes.value.filter { JSONObject(it.json).optString("profile_id") == id }
                repository.deleteBundle(routes.map { "live_audio_route" to it.entityId } + ("live_audio_route_profile" to id))
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = "Routing profile removal is queued for synchronization.", syncError = false)
                done()
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = it.message ?: "Could not remove the routing profile.", syncError = true)
            }
        }
    }

    fun deleteSong(id: String, done: () -> Unit) = deleteRecord("song", id, done)

    fun saveEvent(id: String?, data: JSONObject, kitIds: Set<String>, ensembleIds: Set<String>, contactIds: Set<String>, done: () -> Unit) {
        val eventId = id ?: "event_${UUID.randomUUID().toString().replace("-", "")}"
        viewModelScope.launch {
            runCatching { repository.saveEvent(eventId, data, kitIds, ensembleIds, contactIds) }
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
                if (entityType in setOf("venue", "contact", "ensemble") && !imageUri.isNullOrBlank()) {
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
            .put("playback_mode", draft.playbackMode).put("stop_between_songs", if (draft.stopBetweenSongs) 1 else 0)
        val sections = JSONArray()
        val entries = JSONArray()
        draft.sections.forEachIndexed { index, section ->
            sections.put(JSONObject().put("id", section.id).put("set_list_id", draft.id)
                .put("name", section.name.trim().ifBlank { "Set ${index + 1}" }).put("position", index).put("notes", section.notes.trim()))
            val performanceGroups = mutableMapOf<Pair<String, String>, String>()
            section.entries.forEach { entry ->
                val key = entry.performanceGroupType to entry.performanceGroupName.trim().lowercase()
                entry.performanceGroupId?.takeIf { key.first.isNotBlank() && key.second.isNotBlank() }?.let { performanceGroups.putIfAbsent(key, it) }
            }
            section.entries.forEachIndexed { entryIndex, entry ->
                val groupKey = entry.performanceGroupType to entry.performanceGroupName.trim().lowercase()
                val groupId = if (groupKey.first in setOf("medley", "tribute") && groupKey.second.isNotBlank()) {
                    performanceGroups.getOrPut(groupKey) { "grp_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
                } else null
                entries.put(JSONObject().put("id", entry.id).put("set_list_id", draft.id)
                    .put("section_id", section.id).put("song_id", entry.songId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                    .put("position", entryIndex).put("manual_title", entry.manualTitle.trim())
                    .put("entry_notes", entry.notes.trim())
                    .put("performance_group_id", groupId ?: JSONObject.NULL)
                    .put("performance_group_type", entry.performanceGroupType.takeIf { groupId != null } ?: JSONObject.NULL)
                    .put("performance_group_name", entry.performanceGroupName.trim().takeIf { groupId != null } ?: JSONObject.NULL)
                    .put("performance_attachment_id", entry.performanceAttachmentId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                    .put("playback_attachment_id", entry.playbackAttachmentId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                    .put("transition_mode", entry.transitionMode)
                    .put("pre_roll_ms", entry.preRollMs.coerceIn(0, 60_000)))
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

    fun askWorkspace(question: String, domain: String) {
        _reportState.value = _reportState.value.copy(busy = true, message = "", workspaceResult = null)
        viewModelScope.launch {
            runCatching { repository.askWorkspace(question.trim(), domain.trim().lowercase()) }
                .onSuccess { result ->
                    _reportState.value = _reportState.value.copy(
                        busy = false,
                        workspaceResult = result,
                        message = "Workspace search complete.",
                    )
                }
                .onFailure {
                    _reportState.value = _reportState.value.copy(
                        busy = false,
                        message = it.message ?: "Workspace questions are unavailable while offline.",
                    )
                }
        }
    }

    fun replyCrewAction(actionId: String, reply: String) {
        _reportState.value = _reportState.value.copy(busy = true, message = "Crew is reviewing your reply...")
        viewModelScope.launch {
            runCatching { repository.replyCrewAction(actionId, reply.trim()) }
                .onSuccess { result ->
                    _reportState.value = _reportState.value.copy(
                        busy = false,
                        message = result.optString("message", "Crew saved your reply."),
                    )
                }
                .onFailure {
                    _reportState.value = _reportState.value.copy(
                        busy = false,
                        message = it.message ?: "Crew could not process the reply.",
                    )
                }
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

private fun Throwable.passkeyMessage(fallback: String): String = when {
    this::class.simpleName?.contains("Cancellation", ignoreCase = true) == true -> "Passkey request canceled."
    this::class.simpleName?.contains("NoCredential", ignoreCase = true) == true -> "No Studio Leviathan passkey is available on this device."
    this::class.simpleName?.contains("ProviderConfiguration", ignoreCase = true) == true -> "No compatible passkey provider is available on this device."
    !message.isNullOrBlank() -> message!!
    else -> fallback
}

data class ReportUiState(
    val busy: Boolean = false,
    val message: String = "",
    val onlineOverview: JSONObject? = null,
    val aiResult: JSONObject? = null,
    val workspaceResult: JSONObject? = null,
)

data class WorkspaceMembersUiState(
    val busy: Boolean = false,
    val message: String = "",
    val error: Boolean = false,
    val payload: JSONObject? = null,
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
    val playbackMode: String = "manual",
    val stopBetweenSongs: Boolean = true,
)

data class SetSectionDraft(val id: String, val name: String = "", val notes: String = "", val entries: List<SetEntryDraft> = emptyList())
data class SetEntryDraft(
    val id: String,
    val songId: String? = null,
    val manualTitle: String = "",
    val notes: String = "",
    val performanceAttachmentId: String? = null,
    val performanceGroupId: String? = null,
    val performanceGroupType: String = "",
    val performanceGroupName: String = "",
    val playbackAttachmentId: String? = null,
    val transitionMode: String = "manual",
    val preRollMs: Int = 0,
)

class StudioRackViewModelFactory(
    private val repository: StudioRackRepository,
    private val localLiveCoordinator: LocalLiveCoordinator,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StudioRackViewModel(repository, localLiveCoordinator) as T
}
