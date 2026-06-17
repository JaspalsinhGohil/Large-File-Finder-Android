package com.example.filefinder


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermission()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppUI(viewModel)
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI(viewModel: MainViewModel) {
    val searchState by viewModel.searchState.collectAsState()
    val targetN by viewModel.targetN.collectAsState()
    val directories by viewModel.selectedDirectories.collectAsState()

    var newDirPath by remember { mutableStateOf(Environment.getExternalStorageDirectory().absolutePath) }
    var showDirPicker by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Large File Finder") }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            // Input N
            OutlinedTextField(
                value = targetN,
                onValueChange = { viewModel.setN(it) },
                label = { Text("Number of files (N)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !searchState.isSearching
            )

            Spacer(Modifier.height(16.dp))

            // Directory Management with Visual Picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newDirPath,
                    onValueChange = { newDirPath = it },
                    label = { Text("Directory Path") },
                    modifier = Modifier.weight(1f),
                    enabled = !searchState.isSearching,
                    trailingIcon = {
                        IconButton(onClick = { showDirPicker = true }) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Browse Folders",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    })
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.addDirectory(newDirPath) },
                    enabled = !searchState.isSearching
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selected directories list
            directories.forEach { dir ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            dir.absolutePath,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(
                            onClick = { viewModel.removeDirectory(dir) },
                            enabled = !searchState.isSearching
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Start Button & Progress
            Button(
                onClick = { viewModel.startSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = directories.isNotEmpty() && targetN.isNotEmpty() && !searchState.isSearching
            ) {
                Text(if (searchState.isSearching) "Searching..." else "Start Search")
            }

            if (searchState.isSearching) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Files Scanned: ${searchState.filesScanned}",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            // Results List
            if (searchState.currentResults.isNotEmpty() && !searchState.isSearching) {
                Text(
                    "Results (Top ${searchState.currentResults.size}):",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(searchState.currentResults) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.file.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.file.absolutePath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Size: ${formatSize(item.size)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Directory Picker Dialog
    if (showDirPicker) {
        DirectoryPickerDialog(initialDir = File(newDirPath).takeIf { it.exists() && it.isDirectory }
            ?: Environment.getExternalStorageDirectory(), onDirectoryPicked = { selectedFile ->
            newDirPath = selectedFile.absolutePath
            showDirPicker = false
        }, onDismiss = { showDirPicker = false })
    }
}

@Composable
fun DirectoryPickerDialog(
    initialDir: File, onDirectoryPicked: (File) -> Unit, onDismiss: () -> Unit
) {
    var currentDir by remember { mutableStateOf(initialDir) }

    // Safely fetch subdirectories, sorting alphabetically
    val folders = remember(currentDir) {
        currentDir.listFiles()?.filter { it.isDirectory && !it.isHidden }?.sortedBy { it.name }
            ?: emptyList()
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select Folder") }, text = {
        Column {
            Text(
                "Current: ${currentDir.absolutePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp) // Limits height so it doesn't take over screen
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                // Show 'Up' button if we aren't at the root
                if (currentDir.parentFile != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentDir = currentDir.parentFile!! }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(".. (Go Up)")
                        }
                        HorizontalDivider()
                    }
                }

                // List directories
                items(folders) { folder ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentDir = folder }
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(folder.name)
                    }
                    HorizontalDivider()
                }

                if (folders.isEmpty()) {
                    item {
                        Text(
                            "No subfolders here.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }, confirmButton = {
        Button(onClick = { onDirectoryPicked(currentDir) }) {
            Text("Select This Folder")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

// Utility to format bytes into readable format
fun formatSize(sizeInBytes: Long): String {
    val kb = sizeInBytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.2f KB", kb)
        else -> "$sizeInBytes Bytes"
    }
}