package com.example.rickandmorty.data.repository

import androidx.room.withTransaction
import com.example.rickandmorty.data.local.AppDatabase
import com.example.rickandmorty.data.local.dao.TeamDao
import com.example.rickandmorty.data.local.entity.TeamEntity
import com.example.rickandmorty.data.local.entity.TeamSlotEntity
import com.example.rickandmorty.data.local.relation.TeamWithSlots
import com.example.rickandmorty.domain.model.Team
import com.example.rickandmorty.domain.model.TeamSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepository @Inject constructor(
    private val dao: TeamDao,
    private val database: AppDatabase
) {
    companion object {
        const val MAX_SLOTS = 6
    }

    fun observeTeams(profileId: Long): Flow<List<Team>> =
        dao.observeTeamsWithSlots(profileId).map { list ->
            list.map { it.toDomainTeam() }
        }

    fun observeTeam(teamId: Long): Flow<TeamWithSlots?> = dao.observeTeamWithSlots(teamId)

    fun observeTeamDomain(teamId: Long): Flow<Pair<Team, List<TeamSlot>>?> =
        dao.observeTeamWithSlots(teamId).map { tws ->
            tws?.let {
                Pair(
                    it.toDomainTeam(),
                    it.slots.sortedBy { s -> s.position }.map { s -> s.toDomain() }
                )
            }
        }

    suspend fun createTeam(profileId: Long, name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Team name must not be empty" }
        return dao.insertTeam(
            TeamEntity(profileId = profileId, name = trimmed, createdAt = System.currentTimeMillis())
        )
    }

    suspend fun renameTeam(teamId: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        dao.renameTeam(teamId, trimmed)
    }

    suspend fun deleteTeam(teamId: Long) = dao.deleteTeam(teamId)

    suspend fun addSlot(teamId: Long, pokemonId: Int, note: String? = null): Result<Long> {
        val current = dao.getSlots(teamId)
        if (current.size >= MAX_SLOTS) {
            return Result.failure(IllegalStateException("Team is full (max $MAX_SLOTS slots)"))
        }
        val nextPos = (current.maxOfOrNull { it.position } ?: -1) + 1
        val id = dao.insertSlot(
            TeamSlotEntity(
                teamId = teamId,
                pokemonId = pokemonId,
                position = nextPos,
                slotNote = note?.takeIf { it.isNotBlank() }
            )
        )
        return Result.success(id)
    }

    suspend fun removeSlot(slotId: Long) = dao.deleteSlot(slotId)

    suspend fun reorder(teamId: Long, slotIdsInOrder: List<Long>) {
        // Two-phase reposition to avoid violating unique(teamId, position).
        // Wrapped in a transaction so a process kill in the middle leaves the
        // table consistent (either fully reordered or untouched).
        database.withTransaction {
            val existing = dao.getSlots(teamId)
            val offset = (existing.maxOfOrNull { it.position } ?: 0) + 100
            slotIdsInOrder.forEachIndexed { index, id ->
                dao.updateSlotPosition(id, offset + index)
            }
            slotIdsInOrder.forEachIndexed { index, id ->
                dao.updateSlotPosition(id, index)
            }
        }
    }

    suspend fun updateSlotNote(slotId: Long, note: String?) =
        dao.updateSlotNote(slotId, note?.takeIf { it.isNotBlank() })

    suspend fun countSlots(teamId: Long): Int = dao.countSlots(teamId)
}

private fun TeamWithSlots.toDomainTeam() = Team(
    id = team.id,
    profileId = team.profileId,
    name = team.name,
    createdAt = team.createdAt,
    slotCount = slots.size
)

private fun TeamSlotEntity.toDomain() = TeamSlot(
    id = id,
    teamId = teamId,
    pokemonId = pokemonId,
    position = position,
    note = slotNote
)
