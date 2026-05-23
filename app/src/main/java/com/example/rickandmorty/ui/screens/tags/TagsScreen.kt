package com.example.rickandmorty.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rickandmorty.domain.model.Tag
import com.example.rickandmorty.ui.viewmodel.TagsViewModel

private val TagColors = listOf(
    0xFFE57373.toInt(), 0xFFF06292.toInt(), 0xFFBA68C8.toInt(), 0xFF9575CD.toInt(),
    0xFF7986CB.toInt(), 0xFF64B5F6.toInt(), 0xFF4FC3F7.toInt(), 0xFF4DD0E1.toInt(),
    0xFF4DB6AC.toInt(), 0xFF81C784.toInt(), 0xFFAED581.toInt(), 0xFFFFD54F.toInt(),
    0xFFFFB74D.toInt(), 0xFFFF8A65.toInt(), 0xFFA1887F.toInt(), 0xFF90A4AE.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(viewModel: TagsViewModel = hiltViewModel()) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingEdit by remember { mutableStateOf<Tag?>(null) }
    var pendingDelete by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tags") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New tag") }
            )
        }
    ) { padding ->
        if (tags.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tags yet. Tags help you organize Pokemon by your own categories.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                items(tags, key = { it.id }) { tag ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { pendingEdit = tag }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(tag.colorArgb))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(tag.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { pendingEdit = tag }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { pendingDelete = tag }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        TagEditorDialog(
            initialName = "",
            initialColor = TagColors.first(),
            title = "New tag",
            onDismiss = { showCreate = false },
            onConfirm = { name, color ->
                viewModel.createTag(name, color)
                showCreate = false
            }
        )
    }

    pendingEdit?.let { tag ->
        TagEditorDialog(
            initialName = tag.name,
            initialColor = tag.colorArgb,
            title = "Edit tag",
            onDismiss = { pendingEdit = null },
            onConfirm = { name, color ->
                viewModel.updateTag(tag.copy(name = name, colorArgb = color))
                pendingEdit = null
            }
        )
    }

    pendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete tag '${tag.name}'?") },
            text = { Text("It will be removed from all Pokemon it's assigned to.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tag.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorDialog(
    initialName: String,
    initialColor: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Tag name") }
                )
                Spacer(Modifier.padding(8.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    TagColors.take(8).forEach { c ->
                        ColorSwatch(color = c, selected = c == color, onClick = { color = c })
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    TagColors.drop(8).forEach { c ->
                        ColorSwatch(color = c, selected = c == color, onClick = { color = c })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, color) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ColorSwatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(color))
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Text(
                "✓",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
