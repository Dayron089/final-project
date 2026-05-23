package com.example.rickandmorty.ui.screens.teams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.rickandmorty.data.repository.TeamRepository
import com.example.rickandmorty.ui.viewmodel.TeamDetailViewModel
import com.example.rickandmorty.ui.viewmodel.TeamSlotDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    onBack: () -> Unit,
    onPick: () -> Unit,
    viewModel: TeamDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showRename by remember { mutableStateOf(false) }
    var editingNoteSlotId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.team?.name ?: "Team") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRename = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.slots.size < TeamRepository.MAX_SLOTS) {
                ExtendedFloatingActionButton(
                    onClick = onPick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Pokemon") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "${state.slots.size} / ${TeamRepository.MAX_SLOTS} slots",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
            if (state.slots.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Pokemon in this team yet.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(state.slots, key = { it.slot.id }) { display ->
                        SlotRow(
                            display = display,
                            atTop = state.slots.first().slot.id == display.slot.id,
                            atBottom = state.slots.last().slot.id == display.slot.id,
                            onUp = { viewModel.moveSlot(display.slot.id, -1) },
                            onDown = { viewModel.moveSlot(display.slot.id, +1) },
                            onRemove = { viewModel.removeSlot(display.slot.id) },
                            onEditNote = { editingNoteSlotId = display.slot.id }
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        var draft by remember { mutableStateOf(state.team?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename team") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameTeam(draft)
                        showRename = false
                    },
                    enabled = draft.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
        )
    }

    editingNoteSlotId?.let { slotId ->
        val current = state.slots.firstOrNull { it.slot.id == slotId } ?: return@let
        var draft by remember(slotId) { mutableStateOf(current.slot.note ?: "") }
        AlertDialog(
            onDismissRequest = { editingNoteSlotId = null },
            title = { Text("Slot note") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(slotId, draft.ifBlank { null })
                    editingNoteSlotId = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingNoteSlotId = null }) { Text("Cancel") } }
        )
    }

    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }
}

@Composable
private fun SlotRow(
    display: TeamSlotDisplay,
    atTop: Boolean,
    atBottom: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = display.pokemon?.imageUrl,
                contentDescription = display.pokemon?.name,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    (display.pokemon?.name ?: "#${display.slot.pokemonId}").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                val note = display.slot.note
                if (!note.isNullOrBlank()) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onUp, enabled = !atTop) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                }
                IconButton(onClick = onDown, enabled = !atBottom) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                }
                IconButton(onClick = onEditNote) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove from team")
                }
            }
        }
    }
}
