package com.example.rickandmorty.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.rickandmorty.data.local.entity.TeamEntity
import com.example.rickandmorty.data.local.entity.TeamSlotEntity

data class TeamWithSlots(
    @Embedded val team: TeamEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "teamId"
    )
    val slots: List<TeamSlotEntity>
)
