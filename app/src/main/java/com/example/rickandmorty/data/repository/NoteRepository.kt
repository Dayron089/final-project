package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.NoteDao
import com.example.rickandmorty.data.local.entity.NoteEntity
import com.example.rickandmorty.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao
) {
    fun observe(profileId: Long, pokemonId: Int): Flow<Note?> =
        dao.observe(profileId, pokemonId).map { it?.toDomain() }

    suspend fun save(profileId: Long, pokemonId: Int, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            dao.delete(profileId, pokemonId)
        } else {
            dao.upsert(
                NoteEntity(
                    profileId = profileId,
                    pokemonId = pokemonId,
                    body = trimmed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun delete(profileId: Long, pokemonId: Int) = dao.delete(profileId, pokemonId)
}

private fun NoteEntity.toDomain() = Note(
    profileId = profileId,
    pokemonId = pokemonId,
    body = body,
    updatedAt = updatedAt
)
