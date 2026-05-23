package com.example.rickandmorty.di

import android.content.Context
import androidx.room.Room
import com.example.rickandmorty.data.local.AppDatabase
import com.example.rickandmorty.data.local.dao.FavoritesDao
import com.example.rickandmorty.data.local.dao.HistoryDao
import com.example.rickandmorty.data.local.dao.NoteDao
import com.example.rickandmorty.data.local.dao.PokemonCacheDao
import com.example.rickandmorty.data.local.dao.PresetDao
import com.example.rickandmorty.data.local.dao.ProfileDao
import com.example.rickandmorty.data.local.dao.TagDao
import com.example.rickandmorty.data.local.dao.TeamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pokemon.db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    @Provides fun providePokemonCacheDao(db: AppDatabase): PokemonCacheDao = db.pokemonCacheDao()
    @Provides fun provideFavoritesDao(db: AppDatabase): FavoritesDao = db.favoritesDao()
    @Provides fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun provideTeamDao(db: AppDatabase): TeamDao = db.teamDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun providePresetDao(db: AppDatabase): PresetDao = db.presetDao()
}
