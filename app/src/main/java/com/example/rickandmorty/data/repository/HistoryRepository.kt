package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.HistoryDao
import com.example.rickandmorty.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val dao: HistoryDao
) {
    fun observe(profileId: Long, limit: Int = 100): Flow<List<HistoryEntity>> =
        dao.observeForProfile(profileId, limit)

    suspend fun record(profileId: Long, pokemonId: Int) {
        dao.upsert(HistoryEntity(profileId, pokemonId, System.currentTimeMillis()))
    }

    suspend fun clear(profileId: Long) = dao.clearForProfile(profileId)
}
