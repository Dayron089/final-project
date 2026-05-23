package com.example.rickandmorty.data.repository

import com.example.rickandmorty.data.local.dao.TagDao
import com.example.rickandmorty.data.local.entity.PokemonTagCrossRef
import com.example.rickandmorty.data.local.entity.TagEntity
import com.example.rickandmorty.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val dao: TagDao
) {

    fun observeTags(profileId: Long): Flow<List<Tag>> =
        dao.observeTagsForProfile(profileId).map { it.map { e -> e.toDomain() } }

    fun observeTagsForPokemon(profileId: Long, pokemonId: Int): Flow<List<Tag>> =
        dao.observeTagsForPokemon(profileId, pokemonId).map { it.map { e -> e.toDomain() } }

    fun observePokemonIdsForTagSet(profileId: Long, tagIds: List<Long>): Flow<Set<Int>> =
        if (tagIds.isEmpty()) flowOf(emptySet())
        else dao.observePokemonIdsForTagSet(profileId, tagIds).map { it.toSet() }

    fun observeUsageCount(tagId: Long): Flow<Int> = dao.observeUsageCount(tagId)

    suspend fun createTag(profileId: Long, name: String, colorArgb: Int): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Tag name must not be empty" }
        return dao.insertTag(TagEntity(profileId = profileId, name = trimmed, colorArgb = colorArgb))
    }

    suspend fun updateTag(tag: Tag) {
        dao.updateTag(
            TagEntity(id = tag.id, profileId = tag.profileId, name = tag.name.trim(), colorArgb = tag.colorArgb)
        )
    }

    suspend fun deleteTag(tagId: Long) = dao.deleteTag(tagId)

    suspend fun assign(profileId: Long, pokemonId: Int, tagId: Long) {
        dao.insertCrossRef(PokemonTagCrossRef(tagId = tagId, pokemonId = pokemonId, profileId = profileId))
    }

    suspend fun unassign(pokemonId: Int, tagId: Long) = dao.deleteCrossRef(tagId, pokemonId)
}

private fun TagEntity.toDomain() = Tag(id = id, profileId = profileId, name = name, colorArgb = colorArgb)
