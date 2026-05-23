package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.TeamRepository
import com.example.rickandmorty.domain.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val teams: StateFlow<List<Team>> =
        profileRepository.activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList()) else teamRepository.observeTeams(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun createTeam(name: String) {
        viewModelScope.launch {
            try {
                val profileId = profileRepository.activeProfileId.first()
                if (profileId == -1L) return@launch
                teamRepository.createTeam(profileId, name)
                _error.value = null
            } catch (e: Throwable) {
                _error.value = e.message
            }
        }
    }

    fun renameTeam(teamId: Long, name: String) {
        viewModelScope.launch {
            try {
                teamRepository.renameTeam(teamId, name)
            } catch (e: Throwable) {
                _error.value = e.message
            }
        }
    }

    fun deleteTeam(teamId: Long) {
        viewModelScope.launch { teamRepository.deleteTeam(teamId) }
    }

    fun clearError() { _error.value = null }
}
