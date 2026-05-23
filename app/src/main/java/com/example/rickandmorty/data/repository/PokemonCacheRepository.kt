package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.PokemonCacheDao
import com.example.rickandmorty.data.local.entity.PokemonCacheEntity
import com.example.rickandmorty.data.local.entity.PokemonListCacheEntity
import com.example.rickandmorty.data.preferences.SettingsKeys
import com.example.rickandmorty.data.remote.PokeApiDataSource
import com.example.rickandmorty.domain.model.CachedPokemon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
open class PokemonCacheRepository @Inject constructor(
    private val cacheDao: PokemonCacheDao,
    private val api: PokeApiDataSource,
    private val settings: SettingsRepository
) {

    // Overridable for tests
    protected open fun clock(): Long = System.currentTimeMillis()


    fun observeListRows(): Flow<List<CachedPokemon>> =
        combine(cacheDao.observeListRows(), cacheDao.observeAll()) { listRows, details ->
            val byId = details.associateBy { it.id }
            listRows.map { row ->
                val detail = byId[row.id]
                CachedPokemon(
                    id = row.id,
                    name = row.name,
                    imageUrl = detail?.imageUrl ?: row.imageUrl,
                    types = detail?.types ?: emptyList(),
                    height = detail?.height,
                    weight = detail?.weight,
                    fetchedAt = detail?.fetchedAt ?: row.fetchedAt,
                    detailLoaded = detail != null
                )
            }
        }

    fun observeDetail(id: Int): Flow<CachedPokemon?> =
        cacheDao.observeById(id).map { it?.toDomain() }

    suspend fun refreshList(force: Boolean = false): Result<Unit> {
        val ttlMs = ttlMs()
        if (!force) {
            val lastFetched = cacheDao.lastListFetchedAt() ?: 0L
            if (clock() - lastFetched < ttlMs && cacheDao.listCount() > 0) {
                return Result.success(Unit)
            }
        }
        val limit = SettingsKeys.Defaults.DEFAULT_LIST_PAGE_SIZE
        return api.fetchListPage(limit = limit, offset = 0).map { results ->
            val now = clock()
            val rows = results.map {
                PokemonListCacheEntity(
                    id = it.id,
                    name = it.name,
                    imageUrl = it.imageUrl,
                    fetchedAt = now
                )
            }
            cacheDao.upsertListRows(rows)
            settings.setLastSyncAt(now)
        }
    }

    suspend fun ensureDetailFresh(id: Int): Result<CachedPokemon> {
        val ttlMs = ttlMs()
        val existing = cacheDao.getById(id)
        val now = clock()
        val isFresh = existing != null && now - existing.fetchedAt < ttlMs
        if (isFresh) return Result.success(existing!!.toDomain())

        // Always look up by stable numeric id; cached `name` may be stale.
        val fetched = api.fetchDetailById(id)
        return fetched.fold(
            onSuccess = { detail ->
                val entity = PokemonCacheEntity(
                    id = detail.id,
                    name = detail.name,
                    imageUrl = detail.sprites.frontDefault
                        ?: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${detail.id}.png",
                    types = detail.types.map { it.type.name },
                    height = detail.height,
                    weight = detail.weight,
                    fetchedAt = now
                )
                cacheDao.upsertDetail(entity)
                Result.success(entity.toDomain())
            },
            onFailure = { err ->
                // Graceful degradation only on IO/network failures — for
                // protocol-level errors (HTTP 4xx/5xx, parse failures) bubble
                // up so caller can distinguish "stale data served" from
                // "server actually broken".
                if (existing != null && err is IOException) {
                    Result.success(existing.toDomain())
                } else {
                    Result.failure(err)
                }
            }
        )
    }

    suspend fun refreshStaleDetails(limit: Int = 20): Result<Int> = runCatching {
        val ttlMs = ttlMs()
        val now = clock()
        val staleIds = cacheDao.staleDetailIds(threshold = now - ttlMs, limit = limit)
        var refreshed = 0
        staleIds.forEach { id ->
            ensureDetailFresh(id).onSuccess { refreshed++ }
        }
        refreshed
    }

    suspend fun count(): Int = cacheDao.count()

    private suspend fun ttlMs(): Long {
        val hours = max(1, settings.cacheTtlHours.first())
        return hours.toLong() * 60L * 60L * 1000L
    }
}

private fun PokemonCacheEntity.toDomain() = CachedPokemon(
    id = id,
    name = name,
    imageUrl = imageUrl,
    types = types,
    height = height,
    weight = weight,
    fetchedAt = fetchedAt,
    detailLoaded = true
)
