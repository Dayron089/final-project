package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.TeamRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import com.example.rickandmorty.domain.model.Team
import com.example.rickandmorty.domain.model.TeamSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class TeamSlotDisplay(
    val slot: TeamSlot,
    val pokemon: CachedPokemon?
)

data class TeamDetailUiState(
    val team: Team? = null,
    val slots: List<TeamSlotDisplay> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val cacheRepository: PokemonCacheRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val teamId: Long = checkNotNull(savedStateHandle.get<Long>("teamId")) {
        "TeamDetailViewModel requires teamId"
    }
    private val _error = MutableStateFlow<String?>(null)
    private val reorderMutex = Mutex()

    val cachedPokemons: StateFlow<List<CachedPokemon>> =
        cacheRepository.observeListRows()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TeamDetailUiState> = combine(
        teamRepository.observeTeamDomain(teamId),
        cacheRepository.observeListRows(),
        _error
    ) { pair, all, err ->
        val team = pair?.first
        val slots = pair?.second ?: emptyList()
        val byId = all.associateBy { it.id }
        val display = slots.map { TeamSlotDisplay(slot = it, pokemon = byId[it.pokemonId]) }
        TeamDetailUiState(team = team, slots = display, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamDetailUiState())

    fun addSlot(pokemonId: Int) {
        viewModelScope.launch {
            teamRepository.addSlot(teamId, pokemonId, null)
                .onFailure { _error.value = it.message }
        }
    }

    fun removeSlot(slotId: Long) {
        viewModelScope.launch { teamRepository.removeSlot(slotId) }
    }

    fun moveSlot(slotId: Long, direction: Int) {
        viewModelScope.launch {
            reorderMutex.withLock {
                val current = state.value.slots.map { it.slot }
                val index = current.indexOfFirst { it.id == slotId }
                if (index < 0) return@withLock
                val target = (index + direction).coerceIn(0, current.size - 1)
                if (target == index) return@withLock
                val reordered = current.toMutableList().apply {
                    val item = removeAt(index)
                    add(target, item)
                }
                teamRepository.reorder(teamId, reordered.map { it.id })
            }
        }
    }

    fun updateNote(slotId: Long, note: String?) {
        viewModelScope.launch { teamRepository.updateSlotNote(slotId, note) }
    }

    fun renameTeam(name: String) {
        viewModelScope.launch {
            try {
                teamRepository.renameTeam(teamId, name)
            } catch (e: Throwable) {
                _error.value = e.message
            }
        }
    }

    fun clearError() { _error.value = null }
}
