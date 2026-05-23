package com.example.rickandmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.data.preferences.ThemeMode
import com.example.rickandmorty.data.repository.ProfileRepository
import com.example.rickandmorty.data.repository.SettingsRepository
import com.example.rickandmorty.data.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val cacheTtlHours: Int = 24,
    val syncEnabled: Boolean = true,
    val syncIntervalHours: Int = 12,
    val wifiOnlySync: Boolean = false,
    val lastSyncAt: Long = 0L,
    val activeProfileName: String = "",
    val activeProfileId: Long = -1L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val scheduler: WorkScheduler
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.cacheTtlHours,
        settings.syncEnabled,
        settings.syncIntervalHours,
        settings.wifiOnlySync
    ) { theme, ttl, syncEnabled, interval, wifiOnly ->
        SettingsBundle(theme, ttl, syncEnabled, interval, wifiOnly)
    }.combine(settings.lastSyncAt) { bundle, lastSync ->
        bundle to lastSync
    }.combine(profileRepository.activeProfileId) { (bundle, lastSync), activeId ->
        Triple(bundle, lastSync, activeId)
    }.map { (bundle, lastSync, activeId) ->
        val profileName = if (activeId == -1L) "" else (profileRepository.getById(activeId)?.name ?: "")
        SettingsUiState(
            theme = bundle.theme,
            cacheTtlHours = bundle.ttl,
            syncEnabled = bundle.syncEnabled,
            syncIntervalHours = bundle.interval,
            wifiOnlySync = bundle.wifiOnly,
            lastSyncAt = lastSync,
            activeProfileName = profileName,
            activeProfileId = activeId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setCacheTtl(hours: Int) = viewModelScope.launch { settings.setCacheTtlHours(hours) }
    fun setSyncInterval(hours: Int) = viewModelScope.launch {
        settings.setSyncIntervalHours(hours)
        if (uiState.value.syncEnabled) scheduler.schedulePeriodicSync()
    }
    fun setSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setSyncEnabled(enabled)
        if (enabled) scheduler.schedulePeriodicSync() else scheduler.cancelPeriodicSync()
    }
    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch {
        settings.setWifiOnlySync(enabled)
        if (uiState.value.syncEnabled) scheduler.schedulePeriodicSync()
    }
    fun runSyncNow() = scheduler.enqueueOneTimeSync()

    private data class SettingsBundle(
        val theme: ThemeMode,
        val ttl: Int,
        val syncEnabled: Boolean,
        val interval: Int,
        val wifiOnly: Boolean
    )
}
