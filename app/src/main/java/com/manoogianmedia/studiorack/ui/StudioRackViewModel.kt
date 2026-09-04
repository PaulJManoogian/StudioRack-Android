package com.manoogianmedia.studiorack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manoogianmedia.studiorack.data.CachedRecord
import com.manoogianmedia.studiorack.data.StudioRackRepository
import com.manoogianmedia.studiorack.data.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val syncState: StateFlow<SyncState?> = repository.syncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}

data class StudioRackUiState(val signedIn: Boolean, val busy: Boolean = false, val message: String = "")

class StudioRackViewModelFactory(private val repository: StudioRackRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = StudioRackViewModel(repository) as T
}
