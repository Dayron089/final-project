package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.local.entity.HistoryEntity
import com.example.rickandmorty.data.repository.HistoryRepository
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryItem(
    val pokemon: CachedPokemon,
    val viewedAt: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val cacheRepository: PokemonCacheRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val history: StateFlow<List<HistoryItem>> =
        profileRepository.activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList<HistoryItem>())
            else combine(historyRepository.observe(id), cacheRepository.observeListRows()) { entries, all ->
                val byId = all.associateBy { it.id }
                entries.mapNotNull { e: HistoryEntity ->
                    val pokemon = byId[e.pokemonId] ?: return@mapNotNull null
                    HistoryItem(pokemon = pokemon, viewedAt = e.viewedAt)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch {
            val id = profileRepository.activeProfileId.first()
            if (id != -1L) historyRepository.clear(id)
        }
    }
}
