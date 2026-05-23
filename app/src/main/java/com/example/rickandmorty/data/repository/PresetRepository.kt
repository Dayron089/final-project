package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.PresetDao
import com.example.rickandmorty.data.local.entity.FilterPresetEntity
import com.example.rickandmorty.domain.model.FilterPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val dao: PresetDao
) {
    fun observe(profileId: Long): Flow<List<FilterPreset>> =
        dao.observeForProfile(profileId).map { list -> list.map { it.toDomain() } }

    suspend fun save(
        profileId: Long,
        name: String,
        query: String,
        types: List<String>,
        tagIds: List<Long>,
        favoritesOnly: Boolean
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Preset name must not be empty" }
        return dao.insert(
            FilterPresetEntity(
                profileId = profileId,
                name = trimmed,
                query = query,
                typeCsv = types.joinToString(","),
                tagIdsCsv = tagIds.joinToString(","),
                favoritesOnly = favoritesOnly,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

private fun FilterPresetEntity.toDomain() = FilterPreset(
    id = id,
    profileId = profileId,
    name = name,
    query = query,
    types = if (typeCsv.isBlank()) emptyList() else typeCsv.split(",").filter { it.isNotBlank() },
    tagIds = if (tagIdsCsv.isBlank()) emptyList() else tagIdsCsv.split(",").mapNotNull { it.toLongOrNull() },
    favoritesOnly = favoritesOnly,
    createdAt = createdAt
)
