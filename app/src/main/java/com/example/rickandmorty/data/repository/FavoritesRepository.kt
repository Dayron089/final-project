package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.FavoritesDao
import com.example.rickandmorty.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoritesDao
) {
    fun observe(profileId: Long): Flow<List<FavoriteEntity>> = dao.observeForProfile(profileId)

    fun observeIds(profileId: Long): Flow<Set<Int>> =
        dao.observeIdsForProfile(profileId).map { it.toSet() }

    suspend fun add(profileId: Long, pokemonId: Int) {
        dao.add(FavoriteEntity(profileId = profileId, pokemonId = pokemonId, addedAt = System.currentTimeMillis()))
    }

    suspend fun remove(profileId: Long, pokemonId: Int) = dao.remove(profileId, pokemonId)

    suspend fun toggle(profileId: Long, pokemonId: Int): Boolean {
        return if (dao.countOf(profileId, pokemonId) > 0) {
            dao.remove(profileId, pokemonId)
            false
        } else {
            add(profileId, pokemonId)
            true
        }
    }
}
