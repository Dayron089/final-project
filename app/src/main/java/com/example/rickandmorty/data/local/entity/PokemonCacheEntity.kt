package com.example.rickandmorty.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pokemon_cache",
    indices = [Index(value = ["name"])]
)
data class PokemonCacheEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
    val height: Int,
    val weight: Int,
    val fetchedAt: Long
)
