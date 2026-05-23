package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.repository.FavoritesRepository
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.PresetRepository
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.TagRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import com.example.rickandmorty.domain.model.FilterPreset
import com.example.rickandmorty.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode { ALL, FAVORITES_ONLY }

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Success(val items: List<PokemonListItem>) : ListUiState
    data object Empty : ListUiState
    data class Error(val message: String) : ListUiState
}

data class PokemonListItem(
    val pokemon: CachedPokemon,
    val isFavorite: Boolean
)

data class ListScreenState(
    val list: ListUiState = ListUiState.Loading,
    val query: String = "",
    val viewMode: ViewMode = ViewMode.ALL,
    val selectedTagIds: Set<Long> = emptySet(),
    val tags: List<Tag> = emptyList(),
    val presets: List<FilterPreset> = emptyList(),
    val isRefreshing: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val cacheRepository: PokemonCacheRepository,
    private val favoritesRepository: FavoritesRepository,
    private val tagRepository: TagRepository,
    private val presetRepository: PresetRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(ViewMode.ALL)
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isRefreshing = MutableStateFlow(false)
    private val _initialLoadComplete = MutableStateFlow(false)

    val query: StateFlow<String> = _query.asStateFlow()
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val activeProfileId: Flow<Long> = profileRepository.activeProfileId

    private val favoriteIds: Flow<Set<Int>> =
        activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptySet()) else favoritesRepository.observeIds(id)
        }

    private val tagsForProfile: Flow<List<Tag>> =
        activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList()) else tagRepository.observeTags(id)
        }

    private val presetsForProfile: Flow<List<FilterPreset>> =
        activeProfileId.flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList()) else presetRepository.observe(id)
        }

    private val pokemonIdsForSelectedTags: Flow<Set<Int>?> =
        combine(activeProfileId, _selectedTagIds) { id, tagIds -> id to tagIds }
            .flatMapLatest { (id, tagIds) ->
                if (id == -1L || tagIds.isEmpty()) flowOf(null)
                else tagRepository.observePokemonIdsForTagSet(id, tagIds.toList())
            }

    private val debouncedQuery: Flow<String> = _query.debounce(300).distinctUntilChanged()

    private data class FilterInputs(
        val list: List<CachedPokemon>,
        val favIds: Set<Int>,
        val query: String,
        val mode: ViewMode,
        val tagFilterIds: Set<Int>?
    )

    private val filteredList: Flow<ListUiState> = combine(
        cacheRepository.observeListRows(),
        favoriteIds,
        debouncedQuery,
        _viewMode,
        pokemonIdsForSelectedTags
    ) { list, favIds, q, mode, tagIds ->
        FilterInputs(list, favIds, q, mode, tagIds)
    }.combine(_initialLoadComplete) { inputs, loaded ->
        when {
            inputs.list.isEmpty() && !loaded -> ListUiState.Loading
            inputs.list.isEmpty() -> ListUiState.Empty
            else -> {
                val filtered = inputs.list.asSequence()
                    .filter { inputs.query.isBlank() || it.name.contains(inputs.query.trim(), ignoreCase = true) }
                    .filter { inputs.mode == ViewMode.ALL || inputs.favIds.contains(it.id) }
                    .filter { inputs.tagFilterIds == null || inputs.tagFilterIds.contains(it.id) }
                    .map { PokemonListItem(pokemon = it, isFavorite = inputs.favIds.contains(it.id)) }
                    .toList()
                if (filtered.isEmpty()) ListUiState.Empty else ListUiState.Success(filtered)
            }
        }
    }

    private data class Aux(
        val tags: List<Tag>,
        val presets: List<FilterPreset>,
        val tagIds: Set<Long>,
        val q: String,
        val mode: ViewMode,
        val refreshing: Boolean
    )

    private val auxState: Flow<Aux> = combine(
        tagsForProfile, presetsForProfile, _selectedTagIds, _query, _viewMode
    ) { tags, presets, tagIds, q, mode ->
        Aux(tags, presets, tagIds, q, mode, false)
    }.combine(_isRefreshing) { aux, refreshing -> aux.copy(refreshing = refreshing) }

    val state: StateFlow<ListScreenState> = combine(filteredList, auxState) { listState, aux ->
        ListScreenState(
            list = listState,
            query = aux.q,
            viewMode = aux.mode,
            selectedTagIds = aux.tagIds,
            tags = aux.tags,
            presets = aux.presets,
            isRefreshing = aux.refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListScreenState())

    init {
        viewModelScope.launch {
            cacheRepository.refreshList(force = false)
            _initialLoadComplete.value = true
        }
    }

    fun onQueryChanged(q: String) { _query.value = q }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }
    fun toggleTag(tagId: Long) {
        _selectedTagIds.value = _selectedTagIds.value.toMutableSet().apply {
            if (!add(tagId)) remove(tagId)
        }
    }
    fun clearTagFilter() { _selectedTagIds.value = emptySet() }

    fun applyPreset(preset: FilterPreset) {
        _query.value = preset.query
        _viewMode.value = if (preset.favoritesOnly) ViewMode.FAVORITES_ONLY else ViewMode.ALL
        _selectedTagIds.value = preset.tagIds.toSet()
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            if (profileId == -1L) return@launch
            presetRepository.save(
                profileId = profileId,
                name = name,
                query = _query.value,
                types = emptyList(),
                tagIds = _selectedTagIds.value.toList(),
                favoritesOnly = _viewMode.value == ViewMode.FAVORITES_ONLY
            )
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { presetRepository.delete(id) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            cacheRepository.refreshList(force = true)
            _isRefreshing.value = false
        }
    }
}

