package com.example.rickandmorty.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CACHE_TTL_HOURS = intPreferencesKey("cache_ttl_hours")
    val SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
    val SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
    val WIFI_ONLY_SYNC = booleanPreferencesKey("wifi_only_sync")
    val HAS_PREFETCHED_GEN1 = booleanPreferencesKey("has_prefetched_gen1")
    val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    val DEFAULT_LIST_PAGE_SIZE = intPreferencesKey("default_list_page_size")

    object Defaults {
        const val ACTIVE_PROFILE_ID = -1L
        const val THEME_MODE = "SYSTEM"
        const val CACHE_TTL_HOURS = 24
        const val SYNC_INTERVAL_HOURS = 12
        const val SYNC_ENABLED = true
        const val WIFI_ONLY_SYNC = false
        const val HAS_PREFETCHED_GEN1 = false
        const val LAST_SYNC_AT = 0L
        const val DEFAULT_LIST_PAGE_SIZE = 151
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK;
    companion object {
        fun parse(raw: String?): ThemeMode = when (raw) {
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> SYSTEM
        }
    }
}
