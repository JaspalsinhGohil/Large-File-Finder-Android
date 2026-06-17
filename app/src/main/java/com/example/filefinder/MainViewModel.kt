package com.example.filefinder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val searchEngine = FileSearchEngine()
    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "search_progress_channel"
    private val NOTIFICATION_ID = 101

    val searchState = searchEngine.progressState

    private val _targetN = MutableStateFlow("10")
    val targetN: StateFlow<String> = _targetN.asStateFlow()

    private val _selectedDirectories = MutableStateFlow<List<File>>(emptyList())
    val selectedDirectories: StateFlow<List<File>> = _selectedDirectories.asStateFlow()

    init {
        setupNotificationChannel()
        observeProgressForNotifications()
    }

    fun setN(value: String) {
        _targetN.value = value
    }

    fun addDirectory(path: String) {
        val file = File(path)
        val currentList = _selectedDirectories.value
        // Only add if it's a valid directory and not already in our list
        if (file.exists() && file.isDirectory && !currentList.contains(file)) {
            _selectedDirectories.value = currentList + file
        }
    }

    fun removeDirectory(file: File) {
        _selectedDirectories.value = _selectedDirectories.value.filter { it != file }
    }

    fun startSearch() {
        val nFiles = _targetN.value.toIntOrNull() ?: return
        val dirs = _selectedDirectories.value

        if (dirs.isEmpty()) return

        viewModelScope.launch {
            searchEngine.findLargestFiles(nFiles, dirs)
        }
    }

    private fun observeProgressForNotifications() {
        viewModelScope.launch {
            searchState.collect { state ->
                if (state.isSearching) {
                    showNotification("Searching...", "Scanned ${state.filesScanned} files")
                } else if (state.filesScanned > 0) {
                    showNotification("Search Complete", "Found top ${state.currentResults.size} files.")
                }
            }
        }
    }

    private fun showNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(getApplication(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_search_category_default)
            .setContentTitle(title)
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setOngoing(title == "Searching...") // Prevents user from swiping it away while active
            .setPriority(NotificationCompat.PRIORITY_LOW)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Search Progress",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}