package com.example.rickandmorty.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "pokemonId"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId"), Index("pokemonId")]
)
data class FavoriteEntity(
    val profileId: Long,
    val pokemonId: Int,
    val addedAt: Long
)
