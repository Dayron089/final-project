package com.example.rickandmorty

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rickandmorty.data.local.AppDatabase
import com.example.rickandmorty.data.local.entity.ProfileEntity
import com.example.rickandmorty.data.repository.TagRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepository
    private var profileId: Long = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TagRepository(db.tagDao())
        profileId = db.profileDao().insert(ProfileEntity(name = "T1", avatarSeed = 1, createdAt = 0))
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun assign_thenObserveTagsForPokemon_emitsAssignedTag() = runTest {
        val tagId = repo.createTag(profileId, "starter", 0xFF00FF00.toInt())
        repo.assign(profileId, pokemonId = 25, tagId = tagId)
        val tags = repo.observeTagsForPokemon(profileId, 25).first()
        assertEquals(1, tags.size)
        assertEquals("starter", tags.first().name)
    }

    @Test
    fun unassign_thenObserve_removesTag() = runTest {
        val tagId = repo.createTag(profileId, "starter", 0)
        repo.assign(profileId, 25, tagId)
        repo.unassign(25, tagId)
        val tags = repo.observeTagsForPokemon(profileId, 25).first()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun observePokemonIdsForTagSet_singleTag_emitsExpectedIds() = runTest {
        val a = repo.createTag(profileId, "a", 0)
        val b = repo.createTag(profileId, "b", 0)
        repo.assign(profileId, 1, a)
        repo.assign(profileId, 2, a)
        repo.assign(profileId, 3, b)
        val withA = repo.observePokemonIdsForTagSet(profileId, listOf(a)).first()
        assertEquals(setOf(1, 2), withA)
    }

    @Test
    fun deleteTag_cascadesCrossRefs() = runTest {
        val tagId = repo.createTag(profileId, "starter", 0)
        repo.assign(profileId, 25, tagId)
        repo.deleteTag(tagId)
        val tags = repo.observeTagsForPokemon(profileId, 25).first()
        assertTrue(tags.isEmpty())
    }
}
