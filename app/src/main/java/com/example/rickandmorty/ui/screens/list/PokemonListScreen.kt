package com.example.rickandmorty.ui.screens.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.rickandmorty.domain.model.FilterPreset
import com.example.rickandmorty.domain.model.Tag
import com.example.rickandmorty.ui.viewmodel.ListUiState
import com.example.rickandmorty.ui.viewmodel.PokemonListItem
import com.example.rickandmorty.ui.viewmodel.PokemonListViewModel
import com.example.rickandmorty.ui.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    onPokemonClick: (Int) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: PokemonListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSavePreset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pokedex") },
                actions = {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isRefreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search pokemon") },
                singleLine = true
            )

            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = state.viewMode == ViewMode.ALL,
                    onClick = { viewModel.setViewMode(ViewMode.ALL) },
                    label = { Text("All") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.viewMode == ViewMode.FAVORITES_ONLY,
                    onClick = { viewModel.setViewMode(ViewMode.FAVORITES_ONLY) },
                    label = { Text("Favorites only") }
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { showSavePreset = true },
                    enabled = state.query.isNotEmpty() || state.viewMode == ViewMode.FAVORITES_ONLY || state.selectedTagIds.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Save preset")
                }
            }

            if (state.tags.isNotEmpty()) {
                TagFilterRow(
                    tags = state.tags,
                    selected = state.selectedTagIds,
                    onToggle = viewModel::toggleTag,
                    onClear = viewModel::clearTagFilter
                )
            }

            if (state.presets.isNotEmpty()) {
                PresetsRow(
                    presets = state.presets,
                    onApply = viewModel::applyPreset,
                    onDelete = viewModel::deletePreset
                )
            }

            Box(Modifier.fillMaxSize()) {
                when (val s = state.list) {
                    is ListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is ListUiState.Empty -> Text(
                        "Nothing matches the current filters.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is ListUiState.Error -> Text(
                        s.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    is ListUiState.Success -> PokemonGrid(
                        items = s.items,
                        onClick = onPokemonClick
                    )
                }
            }
        }
    }

    if (showSavePreset) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePreset = false },
            title = { Text("Save current filter") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveCurrentAsPreset(name)
                            showSavePreset = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSavePreset = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterRow(
    tags: List<Tag>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = selected.contains(tag.id),
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) }
            )
        }
        if (selected.isNotEmpty()) {
            item {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetsRow(
    presets: List<FilterPreset>,
    onApply: (FilterPreset) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            AssistChip(
                onClick = { onApply(preset) },
                label = { Text(preset.name) },
                trailingIcon = {
                    IconButton(
                        onClick = { onDelete(preset.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Delete preset",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PokemonGrid(items: List<PokemonListItem>, onClick: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(items, key = { it.pokemon.id }) { item ->
            PokemonRow(item = item, onClick = { onClick(item.pokemon.id) })
        }
    }
}

@Composable
private fun PokemonRow(item: PokemonListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.pokemon.imageUrl,
                contentDescription = item.pokemon.name,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.pokemon.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.pokemon.types.isNotEmpty()) {
                    Text(
                        item.pokemon.types.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!item.pokemon.detailLoaded) {
                    Text(
                        "Details pending sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
