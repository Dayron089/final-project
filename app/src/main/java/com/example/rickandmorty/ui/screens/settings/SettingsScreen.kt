package com.example.rickandmorty.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rickandmorty.data.preferences.ThemeMode
import com.example.rickandmorty.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSwitchProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Active profile", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        state.activeProfileName.ifBlank { "No profile selected" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.padding(8.dp))
                    OutlinedButton(onClick = onSwitchProfile, modifier = Modifier.fillMaxWidth()) {
                        Text("Switch profile")
                    }
                }
            }

            // Theme card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.padding(4.dp))
                    ThemeMode.values().forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.theme == mode,
                                    onClick = { viewModel.setTheme(mode) }
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.theme == mode, onClick = { viewModel.setTheme(mode) })
                            Spacer(Modifier.padding(4.dp))
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            // Cache & sync card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cache & sync", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.padding(4.dp))

                    Text("Cache TTL: ${state.cacheTtlHours}h")
                    Slider(
                        value = state.cacheTtlHours.toFloat(),
                        onValueChange = { viewModel.setCacheTtl(it.toInt()) },
                        valueRange = 1f..72f,
                        steps = 70
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Background sync", modifier = Modifier.weight(1f))
                        Switch(checked = state.syncEnabled, onCheckedChange = viewModel::setSyncEnabled)
                    }

                    if (state.syncEnabled) {
                        Text("Sync interval: ${state.syncIntervalHours}h")
                        Slider(
                            value = state.syncIntervalHours.toFloat(),
                            onValueChange = { viewModel.setSyncInterval(it.toInt()) },
                            valueRange = 1f..48f,
                            steps = 46
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Wi-Fi only", modifier = Modifier.weight(1f))
                            Switch(checked = state.wifiOnlySync, onCheckedChange = viewModel::setWifiOnly)
                        }
                    }

                    Spacer(Modifier.padding(4.dp))
                    Text(
                        "Last sync: " + if (state.lastSyncAt == 0L) "never"
                        else SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(state.lastSyncAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(4.dp))
                    Button(onClick = viewModel::runSyncNow, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Sync, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Sync now")
                    }
                }
            }
        }
    }
}
