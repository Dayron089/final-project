package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.PokemonCacheDao
import com.example.rickandmorty.data.local.entity.PokemonCacheEntity
import com.example.rickandmorty.data.local.entity.PokemonListCacheEntity
import com.example.rickandmorty.data.model.PokemonDetail
import com.example.rickandmorty.data.model.PokemonResult
import com.example.rickandmorty.data.model.Sprites
import com.example.rickandmorty.data.model.TypeInfo
import com.example.rickandmorty.data.model.TypeSlot
import com.example.rickandmorty.data.remote.PokeApiDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonCacheRepositoryTest {

    private lateinit var dao: PokemonCacheDao
    private lateinit var api: PokeApiDataSource
    private lateinit var settings: SettingsRepository

    private var currentTime: Long = 1_000_000_000L
    private val ttlMs = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        api = mockk()
        settings = mockk()
        // Default cache TTL of 24h
        every { settings.cacheTtlHours } returns MutableStateFlow(24)
        coEvery { settings.setLastSyncAt(any()) } just runs
    }

    private fun newRepo() = object : PokemonCacheRepository(dao, api, settings) {
        override fun clock(): Long = currentTime
    }

    @Test
    fun refreshList_whenWithinTtl_doesNotCallApi() = runTest {
        coEvery { dao.lastListFetchedAt() } returns currentTime - 1000
        coEvery { dao.listCount() } returns 150
        val result = newRepo().refreshList(force = false)
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.fetchListPage(any(), any()) }
    }

    @Test
    fun refreshList_whenStale_callsApiAndUpsertsRows() = runTest {
        coEvery { dao.lastListFetchedAt() } returns currentTime - (ttlMs + 1000)
        coEvery { dao.listCount() } returns 150
        coEvery { api.fetchListPage(any(), any()) } returns Result.success(
            listOf(PokemonResult("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1/"))
        )

        val result = newRepo().refreshList(force = false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.fetchListPage(any(), any()) }
        coVerify { dao.upsertListRows(any<List<PokemonListCacheEntity>>()) }
    }

    @Test
    fun refreshList_whenForceTrue_callsApiEvenIfFresh() = runTest {
        coEvery { dao.lastListFetchedAt() } returns currentTime - 1000
        coEvery { dao.listCount() } returns 150
        coEvery { api.fetchListPage(any(), any()) } returns Result.success(emptyList())

        val result = newRepo().refreshList(force = true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.fetchListPage(any(), any()) }
    }

    @Test
    fun ensureDetailFresh_missingRow_fetchesAndCaches() = runTest {
        coEvery { dao.getById(1) } returns null
        coEvery { api.fetchDetailById(1) } returns Result.success(
            PokemonDetail(
                id = 1, name = "bulbasaur", height = 7, weight = 69,
                sprites = Sprites(frontDefault = "url", frontShiny = null),
                types = listOf(TypeSlot(TypeInfo("grass")))
            )
        )

        val result = newRepo().ensureDetailFresh(1)

        assertTrue(result.isSuccess)
        coVerify { dao.upsertDetail(any<PokemonCacheEntity>()) }
    }

    @Test
    fun ensureDetailFresh_freshRow_returnsCachedWithoutNetwork() = runTest {
        coEvery { dao.getById(1) } returns PokemonCacheEntity(
            id = 1, name = "bulbasaur", imageUrl = "u", types = listOf("grass"),
            height = 7, weight = 69, fetchedAt = currentTime - 1000
        )

        val result = newRepo().ensureDetailFresh(1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { api.fetchDetailById(any()) }
        coVerify(exactly = 0) { api.fetchDetail(any()) }
    }

    @Test
    fun ensureDetailFresh_apiFailureWithFreshRow_returnsCache() = runTest {
        coEvery { dao.getById(1) } returns PokemonCacheEntity(
            id = 1, name = "bulbasaur", imageUrl = "u", types = listOf("grass"),
            height = 7, weight = 69, fetchedAt = currentTime - (ttlMs + 1000)
        )
        coEvery { api.fetchDetailById(1) } returns Result.failure(IOException("offline"))

        val result = newRepo().ensureDetailFresh(1)

        assertTrue(result.isSuccess)
        assertEquals("bulbasaur", result.getOrThrow().name)
    }

    @Test
    fun ensureDetailFresh_nonIoFailureWithCache_returnsFailure() = runTest {
        // Non-IO error (e.g. HTTP 500, parse error) should NOT silently
        // serve stale cache — bubble the error so caller can react.
        coEvery { dao.getById(1) } returns PokemonCacheEntity(
            id = 1, name = "bulbasaur", imageUrl = "u", types = listOf("grass"),
            height = 7, weight = 69, fetchedAt = currentTime - (ttlMs + 1000)
        )
        coEvery { api.fetchDetailById(1) } returns Result.failure(IllegalStateException("HTTP 500"))

        val result = newRepo().ensureDetailFresh(1)

        assertTrue(result.isFailure)
    }

    @Test
    fun ensureDetailFresh_apiFailureNoRow_returnsFailure() = runTest {
        coEvery { dao.getById(2) } returns null
        coEvery { api.fetchDetailById(2) } returns Result.failure(IOException("offline"))

        val result = newRepo().ensureDetailFresh(2)

        assertTrue(result.isFailure)
    }
}
