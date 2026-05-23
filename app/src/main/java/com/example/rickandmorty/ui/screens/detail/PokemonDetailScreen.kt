package com.example.rickandmorty.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import coil.compose.AsyncImage
import com.example.rickandmorty.domain.model.Tag
import com.example.rickandmorty.ui.viewmodel.PokemonDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    onBack: () -> Unit,
    viewModel: PokemonDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pokemonId = state.pokemon?.id
    var noteDraft by remember(pokemonId) { mutableStateOf("") }
    var noteInitialized by remember(pokemonId) { mutableStateOf(false) }
    LaunchedEffect(pokemonId, state.note?.body) {
        // Initialize draft from server only once per pokemon, so user's in-progress
        // edits aren't wiped by an unrelated re-emission (#4).
        if (!noteInitialized) {
            noteDraft = state.note?.body ?: ""
            noteInitialized = true
        }
    }
    var showTagPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.pokemon?.name?.replaceFirstChar { it.uppercase() } ?: "Pokemon")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites"
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            val p = state.pokemon
            if (p == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = p.imageUrl,
                            contentDescription = p.name,
                            modifier = Modifier.size(160.dp)
                        )
                        if (p.types.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                p.types.forEach { type ->
                                    ElevatedAssistChip(onClick = {}, label = { Text(type) })
                                }
                            }
                        }
                        if (p.height != null && p.weight != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Height: ${p.height} • Weight: ${p.weight}")
                        }
                    }
                }

                NotesCard(
                    body = noteDraft,
                    onChange = { noteDraft = it },
                    onSave = { viewModel.saveNote(noteDraft) },
                    hasSavedNote = state.note != null
                )

                TagsCard(
                    assigned = state.assignedTags,
                    onOpenPicker = { showTagPicker = true },
                    onRemove = { viewModel.unassignTag(it) }
                )
            }
        }
    }

    if (showTagPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showTagPicker = false },
            sheetState = sheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Tags", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (state.allTags.isEmpty()) {
                    Text(
                        "No tags yet. Create one from the Tags tab.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    state.allTags.forEach { tag ->
                        val assigned = state.assignedTags.any { it.id == tag.id }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(tag.colorArgb))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(tag.name, Modifier.weight(1f))
                            FilterChip(
                                selected = assigned,
                                onClick = {
                                    if (assigned) viewModel.unassignTag(tag.id)
                                    else viewModel.assignTag(tag.id)
                                },
                                label = { Text(if (assigned) "Assigned" else "Assign") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showTagPicker = false }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun NotesCard(
    body: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    hasSavedNote: Boolean
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Personal note", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What do you want to remember about this Pokemon?") }
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSave) {
                    Text(if (hasSavedNote) "Update" else "Save note")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsCard(
    assigned: List<Tag>,
    onOpenPicker: () -> Unit,
    onRemove: (Long) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenPicker) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (assigned.isEmpty()) {
                Text(
                    "No tags assigned.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    assigned.forEach { tag ->
                        ElevatedAssistChip(
                            onClick = { onRemove(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }
        }
    }
}
