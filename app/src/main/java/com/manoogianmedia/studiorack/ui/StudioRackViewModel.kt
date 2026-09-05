package com.manoogianmedia.studiorack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.CachedAttachment
import com.manoogianmedia.studiorack.data.StudioRackRepository
import com.manoogianmedia.studiorack.data.SyncState
import com.manoogianmedia.studiorack.data.SupportingRecord
import com.manoogianmedia.studiorack.data.SyncConflict
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class StudioRackViewModel(private val repository: StudioRackRepository) : ViewModel() {
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
    val syncState: StateFlow<SyncState?> = repository.syncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pendingCount: StateFlow<Int> = repository.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val conflicts: StateFlow<List<SyncConflict>> = repository.conflicts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(StudioRackUiState(repository.signedIn()))
    val uiState: StateFlow<StudioRackUiState> = _uiState.asStateFlow()

    fun signIn(email: String, accessCode: String, mfaCode: String) {
        _uiState.value = _uiState.value.copy(busy = true, message = "")
        viewModelScope.launch {
            runCatching { repository.signIn(email, accessCode, mfaCode) }
                .onSuccess { _uiState.value = StudioRackUiState(signedIn = true, message = "StudioRack is ready offline.") }
                .onFailure { _uiState.value = StudioRackUiState(signedIn = false, message = it.message ?: "Sign-in failed.") }
        }
    }

    fun sync() {
        _uiState.value = _uiState.value.copy(busy = true)
        viewModelScope.launch {
            runCatching { repository.sync() }
                .onSuccess { _uiState.value = _uiState.value.copy(busy = false, message = "Synced.") }
                .onFailure { _uiState.value = _uiState.value.copy(busy = false, message = "Offline: showing the last synchronized data.") }
        }
    }

    fun saveSong(id: String?, data: JSONObject, done: () -> Unit) = saveRecord("song", id ?: "song_${UUID.randomUUID().toString().replace("-", "")}", data, done)

    fun deleteSong(id: String, done: () -> Unit) = deleteRecord("song", id, done)

    fun saveEvent(id: String?, data: JSONObject, done: () -> Unit) = saveRecord("studio_event", id ?: "event_${UUID.randomUUID().toString().replace("-", "")}", data, done)

    fun deleteEvent(id: String, done: () -> Unit) = deleteRecord("studio_event", id, done)

    fun saveSetList(draft: SetListDraft, done: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.saveSetList(
                    draft.id,
                    JSONObject().put("id", draft.id).put("name", draft.name.trim())
                        .put("description", draft.description.trim()).put("notes", draft.notes.trim())
                        .put("print_charts", if (draft.attachmentPrintMode == "none") 0 else 1)
                        .put("attachment_print_mode", draft.attachmentPrintMode).put("is_favorite", if (draft.favorite) 1 else 0),
                    draft.sections.mapIndexed { index, section ->
                        section.id to JSONObject().put("id", section.id).put("set_list_id", draft.id)
                            .put("name", section.name.trim().ifBlank { "Set ${index + 1}" }).put("position", index).put("notes", section.notes.trim())
                    },
                    draft.sections.flatMap { section ->
                        section.entries.mapIndexed { entryIndex, entry ->
                            entry.id to JSONObject().put("id", entry.id).put("set_list_id", draft.id)
                                .put("section_id", section.id).put("song_id", entry.songId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                                .put("position", entryIndex).put("manual_title", entry.manualTitle.trim())
                                .put("entry_notes", entry.notes.trim())
                                .put("performance_attachment_id", entry.performanceAttachmentId?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
                        }
                    },
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = "Set list saved offline. Sync is queued.")
                done()
            }.onFailure { _uiState.value = _uiState.value.copy(message = it.message ?: "Could not save set list.") }
        }
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

data class StudioRackUiState(val signedIn: Boolean, val busy: Boolean = false, val message: String = "")

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

class StudioRackViewModelFactory(private val repository: StudioRackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StudioRackViewModel(repository) as T
}
