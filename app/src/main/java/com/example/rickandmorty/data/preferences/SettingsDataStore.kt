package com.example.rickandmorty.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

const val APP_SETTINGS_DATASTORE = "app_settings"

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = APP_SETTINGS_DATASTORE)
