package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.FavoritesRepository
import com.example.rickandmorty.data.repository.HistoryRepository
import com.example.rickandmorty.data.repository.NoteRepository
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.TagRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import com.example.rickandmorty.domain.model.Note
import com.example.rickandmorty.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val pokemon: CachedPokemon? = null,
    val isFavorite: Boolean = false,
    val note: Note? = null,
    val assignedTags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    private val cacheRepository: PokemonCacheRepository,
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pokemonId: Int = checkNotNull(savedStateHandle.get<Int>("pokemonId")) {
        "PokemonDetailViewModel requires `pokemonId` argument"
    }
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val activeProfileId: Flow<Long> = profileRepository.activeProfileId

    private val pokemonFlow: Flow<CachedPokemon?> = cacheRepository.observeDetail(pokemonId)

    private val favFlow: Flow<Boolean> = activeProfileId.flatMapLatest { id ->
        if (id == -1L) flowOf(false)
        else favoritesRepository.observeIds(id).map { it.contains(pokemonId) }
    }

    private val noteFlow: Flow<Note?> = activeProfileId.flatMapLatest { id ->
        if (id == -1L) flowOf(null) else noteRepository.observe(id, pokemonId)
    }

    private val assignedTagsFlow: Flow<List<Tag>> = activeProfileId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else tagRepository.observeTagsForPokemon(id, pokemonId)
    }

    private val allTagsFlow: Flow<List<Tag>> = activeProfileId.flatMapLatest { id ->
        if (id == -1L) flowOf(emptyList()) else tagRepository.observeTags(id)
    }

    val state: StateFlow<DetailUiState> = combine(
        pokemonFlow, favFlow, noteFlow, assignedTagsFlow, allTagsFlow
    ) { pokemon, fav, note, assigned, all ->
        Quintuple(pokemon, fav, note, assigned, all)
    }.combine(_isRefreshing) { q, refreshing -> q to refreshing }
        .combine(_error) { (q, refreshing), error ->
            DetailUiState(
                pokemon = q.a,
                isFavorite = q.b,
                note = q.c,
                assignedTags = q.d,
                allTags = q.e,
                isRefreshing = refreshing,
                error = error
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch {
            _isRefreshing.value = true
            val profileId = activeProfileId.first()
            if (profileId != -1L) historyRepository.record(profileId, pokemonId)
            cacheRepository.ensureDetailFresh(pokemonId)
                .onFailure { _error.value = it.message }
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            if (profileId != -1L) favoritesRepository.toggle(profileId, pokemonId)
        }
    }

    fun saveNote(body: String) {
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            if (profileId != -1L) noteRepository.save(profileId, pokemonId, body)
        }
    }

    fun assignTag(tagId: Long) {
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            if (profileId != -1L) tagRepository.assign(profileId, pokemonId, tagId)
        }
    }

    fun unassignTag(tagId: Long) {
        viewModelScope.launch { tagRepository.unassign(pokemonId, tagId) }
    }

    fun retry() {
        viewModelScope.launch {
            _error.value = null
            _isRefreshing.value = true
            cacheRepository.ensureDetailFresh(pokemonId)
                .onFailure { _error.value = it.message }
            _isRefreshing.value = false
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val a: A, val b: B, val c: C, val d: D, val e: E
    )
}
