package com.example.rickandmorty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.rickandmorty.data.local.entity.TeamEntity
import com.example.rickandmorty.data.local.entity.TeamSlotEntity
import com.example.rickandmorty.data.local.relation.TeamWithSlots
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeTeamsForProfile(profileId: Long): Flow<List<TeamEntity>>

    @Transaction
    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun observeTeamWithSlots(teamId: Long): Flow<TeamWithSlots?>

    @Transaction
    @Query("SELECT * FROM teams WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun observeTeamsWithSlots(profileId: Long): Flow<List<TeamWithSlots>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTeam(entity: TeamEntity): Long

    @Query("UPDATE teams SET name = :name WHERE id = :id")
    suspend fun renameTeam(id: Long, name: String)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteTeam(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(entity: TeamSlotEntity): Long

    @Query("DELETE FROM team_slots WHERE id = :slotId")
    suspend fun deleteSlot(slotId: Long)

    @Query("UPDATE team_slots SET position = :position WHERE id = :slotId")
    suspend fun updateSlotPosition(slotId: Long, position: Int)

    @Query("UPDATE team_slots SET slotNote = :note WHERE id = :slotId")
    suspend fun updateSlotNote(slotId: Long, note: String?)

    @Query("SELECT COUNT(*) FROM team_slots WHERE teamId = :teamId")
    suspend fun countSlots(teamId: Long): Int

    @Query("SELECT * FROM team_slots WHERE teamId = :teamId ORDER BY position ASC")
    suspend fun getSlots(teamId: Long): List<TeamSlotEntity>
}
