package com.example.rickandmorty

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rickandmorty.data.local.AppDatabase
import com.example.rickandmorty.data.local.entity.ProfileEntity
import com.example.rickandmorty.data.repository.TeamRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeamRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TeamRepository
    private var profileId: Long = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TeamRepository(db.teamDao(), db)
        profileId = db.profileDao().insert(ProfileEntity(name = "T1", avatarSeed = 1, createdAt = 0))
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun addSlot_atSix_returnsFailure() = runTest {
        val teamId = repo.createTeam(profileId, "Squad")
        repeat(6) { repo.addSlot(teamId, 100 + it) }
        val result = repo.addSlot(teamId, 999)
        assertTrue(result.isFailure)
        assertEquals(6, repo.countSlots(teamId))
    }

    @Test
    fun addSlot_underSix_assignsNextPosition() = runTest {
        val teamId = repo.createTeam(profileId, "Squad")
        repo.addSlot(teamId, 1)
        repo.addSlot(teamId, 2)
        val slots = db.teamDao().getSlots(teamId)
        assertEquals(2, slots.size)
        assertEquals(0, slots[0].position)
        assertEquals(1, slots[1].position)
    }

    @Test
    fun reorder_preservesIds_updatesPositionsAtomically() = runTest {
        val teamId = repo.createTeam(profileId, "Squad")
        repo.addSlot(teamId, 1)
        repo.addSlot(teamId, 2)
        repo.addSlot(teamId, 3)
        val slots = db.teamDao().getSlots(teamId)
        val reverseIds = slots.reversed().map { it.id }
        repo.reorder(teamId, reverseIds)
        val after = db.teamDao().getSlots(teamId)
        assertEquals(reverseIds, after.sortedBy { it.position }.map { it.id })
    }

    @Test
    fun deleteTeam_cascadesSlots() = runTest {
        val teamId = repo.createTeam(profileId, "Squad")
        repo.addSlot(teamId, 1)
        repo.addSlot(teamId, 2)
        repo.deleteTeam(teamId)
        assertNull(db.teamDao().observeTeamWithSlots(teamId).first())
        assertEquals(0, db.teamDao().getSlots(teamId).size)
    }
}
