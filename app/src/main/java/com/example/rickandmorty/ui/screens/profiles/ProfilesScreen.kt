package com.example.rickandmorty.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rickandmorty.domain.model.Profile
import com.example.rickandmorty.ui.viewmodel.ProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    onProfileActivated: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }
    var pendingRename by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.activationEvents.collect { onProfileActivated() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choose trainer") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New profile") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.profiles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Create your first trainer profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Each profile keeps separate teams, tags, notes and history.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.profiles, key = { it.id }) { profile ->
                        ProfileRow(
                            profile = profile,
                            active = profile.id == state.activeProfileId,
                            onActivate = { viewModel.setActive(profile.id) },
                            onRenameClick = { pendingRename = profile },
                            onDeleteClick = { pendingDelete = profile }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        ProfileEditDialog(
            initialName = "",
            title = "New profile",
            onDismiss = { showAdd = false },
            onConfirm = {
                viewModel.create(it)
                showAdd = false
            }
        )
    }
    pendingRename?.let { profile ->
        ProfileEditDialog(
            initialName = profile.name,
            title = "Rename profile",
            onDismiss = { pendingRename = null },
            onConfirm = {
                viewModel.rename(profile.id, it)
                pendingRename = null
            }
        )
    }
    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete profile?") },
            text = { Text("All teams, tags, notes and history of '${profile.name}' will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    state.error?.let { msg ->
        LaunchedEffect(msg) { viewModel.clearError() }
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    active: Boolean,
    onActivate: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color((0xFF000000L or (profile.avatarSeed.toLong() and 0xFFFFFFL)).toInt()))
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (active) "Active" else "Tap to activate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (active) {
                Icon(Icons.Filled.Check, contentDescription = "Active")
            } else {
                TextButton(onClick = onActivate) { Text("Use") }
            }
            IconButton(onClick = onRenameClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditDialog(
    initialName: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Profile name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
