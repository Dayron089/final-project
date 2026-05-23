package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rickandmorty.data.local.entity.PokemonTagCrossRef
import com.example.rickandmorty.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags WHERE profileId = :profileId ORDER BY name ASC")
    fun observeTagsForProfile(profileId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: PokemonTagCrossRef)

    @Query("DELETE FROM pokemon_tag_cross_ref WHERE tagId = :tagId AND pokemonId = :pokemonId")
    suspend fun deleteCrossRef(tagId: Long, pokemonId: Int)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN pokemon_tag_cross_ref x ON x.tagId = t.id
        WHERE t.profileId = :profileId AND x.pokemonId = :pokemonId
        ORDER BY t.name ASC
    """)
    fun observeTagsForPokemon(profileId: Long, pokemonId: Int): Flow<List<TagEntity>>

    @Query("SELECT DISTINCT pokemonId FROM pokemon_tag_cross_ref WHERE profileId = :profileId AND tagId IN (:tagIds)")
    fun observePokemonIdsForTagSet(profileId: Long, tagIds: List<Long>): Flow<List<Int>>

    @Query("SELECT COUNT(DISTINCT pokemonId) FROM pokemon_tag_cross_ref WHERE tagId = :tagId")
    fun observeUsageCount(tagId: Long): Flow<Int>
}
