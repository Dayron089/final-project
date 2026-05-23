package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long = -1L,
    val error: String? = null
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _activationEvents = Channel<Unit>(Channel.BUFFERED)
    val activationEvents: Flow<Unit> = _activationEvents.receiveAsFlow()

    val uiState: StateFlow<ProfilesUiState> = combine(
        repo.profiles,
        repo.activeProfileId,
        _error
    ) { profiles, activeId, err ->
        ProfilesUiState(profiles = profiles, activeProfileId = activeId, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun create(name: String) {
        viewModelScope.launch {
            try {
                val id = repo.createProfile(name)
                if (uiState.value.activeProfileId == -1L) {
                    repo.setActive(id)
                    _activationEvents.trySend(Unit)
                }
                _error.value = null
            } catch (e: Throwable) {
                _error.value = e.message ?: "Failed to create profile"
            }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch {
            try {
                repo.renameProfile(id, name)
                _error.value = null
            } catch (e: Throwable) {
                _error.value = e.message ?: "Failed to rename profile"
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteProfile(id) }
    }

    fun setActive(id: Long) {
        viewModelScope.launch {
            repo.setActive(id)
            _activationEvents.trySend(Unit)
        }
    }

    fun clearError() { _error.value = null }
}
