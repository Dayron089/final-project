package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.FavoritesRepository
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val cacheRepository: PokemonCacheRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val favorites: StateFlow<List<CachedPokemon>> =
        profileRepository.activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList<CachedPokemon>())
            else combine(favoritesRepository.observeIds(id), cacheRepository.observeListRows()) { ids, all ->
                all.filter { it.id in ids }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
