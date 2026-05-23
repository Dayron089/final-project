package com.example.rickandmorty.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.rickandmorty.data.preferences.SettingsKeys
import com.example.rickandmorty.data.preferences.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        ThemeMode.parse(it[SettingsKeys.THEME_MODE] ?: SettingsKeys.Defaults.THEME_MODE)
    }

    val cacheTtlHours: Flow<Int> = dataStore.data.map {
        it[SettingsKeys.CACHE_TTL_HOURS] ?: SettingsKeys.Defaults.CACHE_TTL_HOURS
    }

    val syncIntervalHours: Flow<Int> = dataStore.data.map {
        it[SettingsKeys.SYNC_INTERVAL_HOURS] ?: SettingsKeys.Defaults.SYNC_INTERVAL_HOURS
    }

    val syncEnabled: Flow<Boolean> = dataStore.data.map {
        it[SettingsKeys.SYNC_ENABLED] ?: SettingsKeys.Defaults.SYNC_ENABLED
    }

    val wifiOnlySync: Flow<Boolean> = dataStore.data.map {
        it[SettingsKeys.WIFI_ONLY_SYNC] ?: SettingsKeys.Defaults.WIFI_ONLY_SYNC
    }

    val lastSyncAt: Flow<Long> = dataStore.data.map {
        it[SettingsKeys.LAST_SYNC_AT] ?: SettingsKeys.Defaults.LAST_SYNC_AT
    }

    val hasPrefetchedGen1: Flow<Boolean> = dataStore.data.map {
        it[SettingsKeys.HAS_PREFETCHED_GEN1] ?: SettingsKeys.Defaults.HAS_PREFETCHED_GEN1
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode.name }
    }

    suspend fun setCacheTtlHours(hours: Int) {
        dataStore.edit { it[SettingsKeys.CACHE_TTL_HOURS] = hours.coerceIn(1, 24 * 30) }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        dataStore.edit { it[SettingsKeys.SYNC_INTERVAL_HOURS] = hours.coerceIn(1, 24 * 7) }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.SYNC_ENABLED] = enabled }
    }

    suspend fun setWifiOnlySync(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.WIFI_ONLY_SYNC] = enabled }
    }

    suspend fun setLastSyncAt(timestamp: Long) {
        dataStore.edit { it[SettingsKeys.LAST_SYNC_AT] = timestamp }
    }

    suspend fun setHasPrefetchedGen1(value: Boolean) {
        dataStore.edit { it[SettingsKeys.HAS_PREFETCHED_GEN1] = value }
    }
}
