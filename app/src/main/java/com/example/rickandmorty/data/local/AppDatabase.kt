package com.example.rickandmorty.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.rickandmorty.data.local.dao.FavoritesDao
import com.example.rickandmorty.data.local.dao.HistoryDao
import com.example.rickandmorty.data.local.dao.NoteDao
import com.example.rickandmorty.data.local.dao.PokemonCacheDao
import com.example.rickandmorty.data.local.dao.PresetDao
import com.example.rickandmorty.data.local.dao.ProfileDao
import com.example.rickandmorty.data.local.dao.TagDao
import com.example.rickandmorty.data.local.dao.TeamDao
import com.example.rickandmorty.data.local.entity.FavoriteEntity
import com.example.rickandmorty.data.local.entity.FilterPresetEntity
import com.example.rickandmorty.data.local.entity.HistoryEntity
import com.example.rickandmorty.data.local.entity.NoteEntity
import com.example.rickandmorty.data.local.entity.PokemonCacheEntity
import com.example.rickandmorty.data.local.entity.PokemonListCacheEntity
import com.example.rickandmorty.data.local.entity.PokemonTagCrossRef
import com.example.rickandmorty.data.local.entity.ProfileEntity
import com.example.rickandmorty.data.local.entity.TagEntity
import com.example.rickandmorty.data.local.entity.TeamEntity
import com.example.rickandmorty.data.local.entity.TeamSlotEntity

@Database(
    entities = [
        ProfileEntity::class,
        PokemonCacheEntity::class,
        PokemonListCacheEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        TeamEntity::class,
        TeamSlotEntity::class,
        TagEntity::class,
        PokemonTagCrossRef::class,
        NoteEntity::class,
        FilterPresetEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun pokemonCacheDao(): PokemonCacheDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun teamDao(): TeamDao
    abstract fun tagDao(): TagDao
    abstract fun noteDao(): NoteDao
    abstract fun presetDao(): PresetDao
}
