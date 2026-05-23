package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rickandmorty.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE profileId = :profileId AND pokemonId = :pokemonId LIMIT 1")
    fun observe(profileId: Long, pokemonId: Int): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)

    @Query("DELETE FROM notes WHERE profileId = :profileId AND pokemonId = :pokemonId")
    suspend fun delete(profileId: Long, pokemonId: Int)
}
