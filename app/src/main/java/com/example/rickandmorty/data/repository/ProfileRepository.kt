package com.example.rickandmorty.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.rickandmorty.data.local.dao.ProfileDao
import com.example.rickandmorty.data.local.entity.ProfileEntity
import com.example.rickandmorty.data.preferences.SettingsKeys
import com.example.rickandmorty.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val dataStore: DataStore<Preferences>
) {

    val activeProfileId: Flow<Long> = dataStore.data
        .map { it[SettingsKeys.ACTIVE_PROFILE_ID] ?: SettingsKeys.Defaults.ACTIVE_PROFILE_ID }
        .distinctUntilChanged()

    val profiles: Flow<List<Profile>> = profileDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun createProfile(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Profile name must not be empty" }
        val entity = ProfileEntity(
            name = trimmed,
            avatarSeed = Random.nextInt(0, Int.MAX_VALUE),
            createdAt = System.currentTimeMillis()
        )
        return profileDao.insert(entity)
    }

    suspend fun renameProfile(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Profile name must not be empty" }
        profileDao.rename(id, trimmed)
    }

    suspend fun deleteProfile(id: Long) {
        // Flip the active id off the about-to-be-deleted row FIRST. If the
        // process is killed between the two operations, DataStore points to
        // -1L (profile picker shows) and the orphan profile row still exists
        // but is harmless. Reversing the order could leave a dangling
        // active id whose FK-scoped inserts would fail.
        dataStore.edit { prefs ->
            if (prefs[SettingsKeys.ACTIVE_PROFILE_ID] == id) {
                prefs[SettingsKeys.ACTIVE_PROFILE_ID] = SettingsKeys.Defaults.ACTIVE_PROFILE_ID
            }
        }
        profileDao.delete(id)
    }

    suspend fun setActive(id: Long) {
        dataStore.edit { it[SettingsKeys.ACTIVE_PROFILE_ID] = id }
    }

    suspend fun getOrCreateDefault(): Long {
        if (profileDao.count() == 0) {
            val id = createProfile("Trainer")
            setActive(id)
            return id
        }
        // If there are profiles but no active selected, do nothing — let UI route to picker
        return -1L
    }

    suspend fun getById(id: Long): Profile? = profileDao.getById(id)?.toDomain()
}

private fun ProfileEntity.toDomain() = Profile(
    id = id,
    name = name,
    avatarSeed = avatarSeed,
    createdAt = createdAt
)
