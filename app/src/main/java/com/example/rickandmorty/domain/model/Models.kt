package com.example.rickandmorty.domain.model

data class Profile(
    val id: Long,
    val name: String,
    val avatarSeed: Int,
    val createdAt: Long
)

data class CachedPokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
    val height: Int?,
    val weight: Int?,
    val fetchedAt: Long,
    val detailLoaded: Boolean
)

data class Team(
    val id: Long,
    val profileId: Long,
    val name: String,
    val createdAt: Long,
    val slotCount: Int
)

data class TeamSlot(
    val id: Long,
    val teamId: Long,
    val pokemonId: Int,
    val position: Int,
    val note: String?
)

data class Tag(
    val id: Long,
    val profileId: Long,
    val name: String,
    val colorArgb: Int
)

data class FilterPreset(
    val id: Long,
    val profileId: Long,
    val name: String,
    val query: String,
    val types: List<String>,
    val tagIds: List<Long>,
    val favoritesOnly: Boolean,
    val createdAt: Long
)

data class Note(
    val profileId: Long,
    val pokemonId: Int,
    val body: String,
    val updatedAt: Long
)
