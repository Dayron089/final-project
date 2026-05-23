package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rickandmorty.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {

    @Query("SELECT * FROM favorites WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun observeForProfile(profileId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT pokemonId FROM favorites WHERE profileId = :profileId")
    fun observeIdsForProfile(profileId: Long): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId = :profileId AND pokemonId = :pokemonId")
    suspend fun remove(profileId: Long, pokemonId: Int)

    @Query("SELECT COUNT(*) FROM favorites WHERE profileId = :profileId AND pokemonId = :pokemonId")
    suspend fun countOf(profileId: Long, pokemonId: Int): Int
}
