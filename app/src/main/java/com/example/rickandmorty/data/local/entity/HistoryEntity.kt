package com.example.rickandmorty.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "history",
    primaryKeys = ["profileId", "pokemonId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("viewedAt")]
)
data class HistoryEntity(
    val profileId: Long,
    val pokemonId: Int,
    val viewedAt: Long
)
