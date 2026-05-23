package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rickandmorty.data.local.entity.PokemonCacheEntity
import com.example.rickandmorty.data.local.entity.PokemonListCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonCacheDao {

    @Query("SELECT * FROM pokemon_cache ORDER BY id ASC")
    fun observeAll(): Flow<List<PokemonCacheEntity>>

    @Query("SELECT * FROM pokemon_list_cache ORDER BY id ASC")
    fun observeListRows(): Flow<List<PokemonListCacheEntity>>

    @Query("SELECT * FROM pokemon_cache WHERE id = :id")
    fun observeById(id: Int): Flow<PokemonCacheEntity?>

    @Query("SELECT * FROM pokemon_cache WHERE id = :id")
    suspend fun getById(id: Int): PokemonCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetail(entity: PokemonCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListRows(rows: List<PokemonListCacheEntity>)

    @Query("SELECT id FROM pokemon_cache WHERE fetchedAt < :threshold ORDER BY fetchedAt ASC LIMIT :limit")
    suspend fun staleDetailIds(threshold: Long, limit: Int): List<Int>

    @Query("SELECT COUNT(*) FROM pokemon_cache")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM pokemon_list_cache")
    suspend fun listCount(): Int

    @Query("SELECT MAX(fetchedAt) FROM pokemon_list_cache")
    suspend fun lastListFetchedAt(): Long?
}
