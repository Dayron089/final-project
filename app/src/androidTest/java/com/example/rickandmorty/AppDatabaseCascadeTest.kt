package com.example.rickandmorty

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rickandmorty.data.local.AppDatabase
import com.example.rickandmorty.data.local.entity.FavoriteEntity
import com.example.rickandmorty.data.local.entity.HistoryEntity
import com.example.rickandmorty.data.local.entity.PokemonTagCrossRef
import com.example.rickandmorty.data.local.entity.ProfileEntity
import com.example.rickandmorty.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseCascadeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() { db.close() }

    @Test
    fun favoritesAndHistory_cascadeOnProfileDelete() = runTest {
        val pid = db.profileDao().insert(ProfileEntity(name = "A", avatarSeed = 1, createdAt = 0))
        db.favoritesDao().add(FavoriteEntity(pid, 25, 0))
        db.historyDao().upsert(HistoryEntity(pid, 25, 0))

        db.profileDao().delete(pid)

        assertTrue(db.favoritesDao().observeIdsForProfile(pid).first().isEmpty())
        assertTrue(db.historyDao().observeForProfile(pid).first().isEmpty())
    }

    @Test
    fun pokemonTagCrossRef_cascadeOnTagDelete() = runTest {
        val pid = db.profileDao().insert(ProfileEntity(name = "A", avatarSeed = 1, createdAt = 0))
        val tagId = db.tagDao().insertTag(TagEntity(profileId = pid, name = "x", colorArgb = 0))
        db.tagDao().insertCrossRef(PokemonTagCrossRef(tagId = tagId, pokemonId = 25, profileId = pid))

        db.tagDao().deleteTag(tagId)

        assertTrue(db.tagDao().observeTagsForPokemon(pid, 25).first().isEmpty())
    }
}
