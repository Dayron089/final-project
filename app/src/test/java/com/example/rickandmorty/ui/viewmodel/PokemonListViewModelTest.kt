package com.example.rickandmorty.ui.viewmodel

import com.example.rickandmorty.data.repository.FavoritesRepository
import com.example.rickandmorty.data.repository.PokemonCacheRepository
import com.example.rickandmorty.data.repository.PresetRepository
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.TagRepository
import com.example.rickandmorty.domain.model.CachedPokemon
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListViewModelTest {

    private lateinit var cache: PokemonCacheRepository
    private lateinit var favorites: FavoritesRepository
    private lateinit var tags: TagRepository
    private lateinit var presets: PresetRepository
    private lateinit var profiles: ProfileRepository

    private val activeProfileId = MutableStateFlow(1L)
    private val pokemons = MutableStateFlow(
        listOf(
            sample(1, "bulbasaur"),
            sample(4, "charmander"),
            sample(25, "pikachu")
        )
    )
    private val favoriteIds = MutableStateFlow<Set<Int>>(emptySet())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cache = mockk(relaxed = true)
        favorites = mockk(relaxed = true)
        tags = mockk(relaxed = true)
        presets = mockk(relaxed = true)
        profiles = mockk(relaxed = true)

        every { profiles.activeProfileId } returns activeProfileId
        every { cache.observeListRows() } returns pokemons
        every { favorites.observeIds(any()) } returns favoriteIds
        every { tags.observeTags(any()) } returns flowOf(emptyList())
        every { presets.observe(any()) } returns flowOf(emptyList())
        every { tags.observePokemonIdsForTagSet(any(), any()) } returns flowOf(emptySet())
        coEvery { cache.refreshList(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newVm() = PokemonListViewModel(cache, favorites, tags, presets, profiles)

    @Test
    fun successState_emitsItemsFromCache() = runTest {
        val vm = newVm()
        val state = withTimeout(2_000) {
            vm.state.first { it.list is ListUiState.Success }
        }
        val items = (state.list as ListUiState.Success).items
        assertEquals(3, items.size)
    }

    @Test
    fun searchQueryChange_debouncesAndUpdatesList() = runTest {
        val vm = newVm()
        // wait for initial success
        vm.state.first { it.list is ListUiState.Success }
        vm.onQueryChanged("pika")
        val state = withTimeout(2_000) {
            vm.state.first { it.query == "pika" && it.list is ListUiState.Success && (it.list as ListUiState.Success).items.size == 1 }
        }
        val names = (state.list as ListUiState.Success).items.map { it.pokemon.name }
        assertEquals(listOf("pikachu"), names)
    }

    @Test
    fun viewModeFavorites_filtersList() = runTest {
        favoriteIds.value = setOf(25)
        val vm = newVm()
        vm.setViewMode(ViewMode.FAVORITES_ONLY)
        val state = withTimeout(2_000) {
            vm.state.first {
                it.viewMode == ViewMode.FAVORITES_ONLY &&
                    it.list is ListUiState.Success &&
                    (it.list as ListUiState.Success).items.size == 1
            }
        }
        assertEquals(setOf(25), (state.list as ListUiState.Success).items.map { it.pokemon.id }.toSet())
    }

    @Test
    fun applyPreset_updatesAllInputsAtomically() = runTest {
        val vm = newVm()
        vm.state.first { it.list is ListUiState.Success }
        val preset = com.example.rickandmorty.domain.model.FilterPreset(
            id = 1, profileId = 1, name = "p", query = "char",
            types = emptyList(), tagIds = emptyList(), favoritesOnly = false, createdAt = 0
        )
        vm.applyPreset(preset)
        val state = withTimeout(2_000) {
            vm.state.first {
                it.query == "char" &&
                    it.list is ListUiState.Success &&
                    (it.list as ListUiState.Success).items.size == 1
            }
        }
        assertTrue(state.list is ListUiState.Success)
        assertEquals(
            listOf("charmander"),
            (state.list as ListUiState.Success).items.map { it.pokemon.name }
        )
    }

    private fun sample(id: Int, name: String) = CachedPokemon(
        id = id, name = name,
        imageUrl = "",
        types = emptyList(),
        height = null, weight = null,
        fetchedAt = 0, detailLoaded = false
    )
}
