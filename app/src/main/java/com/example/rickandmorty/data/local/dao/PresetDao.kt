package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rickandmorty.data.local.entity.FilterPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Query("SELECT * FROM filter_presets WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeForProfile(profileId: Long): Flow<List<FilterPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FilterPresetEntity): Long

    @Query("DELETE FROM filter_presets WHERE id = :id")
    suspend fun delete(id: Long)
}
