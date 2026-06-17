package com.example.filefinder

import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.PriorityQueue
import java.util.Stack

// @Keep prevents ProGuard from obfuscating this data class in Release builds
@Keep
data class FileItem(val file: File, val size: Long)

data class SearchState(
    val isSearching: Boolean = false,
    val filesScanned: Int = 0,
    val currentResults: List<FileItem> = emptyList()
)

class FileSearchEngine {

    private val TAG = "FileSearchEngine"

    private val _progressState = MutableStateFlow(SearchState())
    val progressState: StateFlow<SearchState> = _progressState.asStateFlow()

    suspend fun findLargestFiles(n: Int, directories: List<File>): List<FileItem> = withContext(Dispatchers.Default) {
        _progressState.value = SearchState(isSearching = true, filesScanned = 0)
        var totalScanned = 0

        // Launch concurrent jobs for each root directory chosen by the user
        val deferredJobs = directories.map { rootDir ->
            async(Dispatchers.IO) {
                scanDirectory(rootDir, n) {
                    totalScanned++
                    // Throttle UI updates so we don't choke the main thread
                    if (totalScanned % 200 == 0) {
                        _progressState.value = _progressState.value.copy(filesScanned = totalScanned)
                    }
                }
            }
        }

        // Wait for all IO threads to finish scanning
        val allHeaps = deferredJobs.awaitAll()

        // Merge the results. We use another Min-Heap here to efficiently merge the top N of each directory.
        val finalHeap = PriorityQueue<FileItem>(n, compareBy { it.size })
        for (heap in allHeaps) {
            for (item in heap) {
                if (finalHeap.size < n) {
                    finalHeap.add(item)
                } else if (item.size > (finalHeap.peek()?.size ?: 0L)) {
                    finalHeap.poll()
                    finalHeap.add(item)
                }
            }
        }

        val resultList = finalHeap.toList().sortedByDescending { it.size }
        _progressState.value = SearchState(isSearching = false, filesScanned = totalScanned, currentResults = resultList)

        Log.d(TAG, "Search complete. Scanned $totalScanned files.")
        return@withContext resultList
    }

    /**
     * Scans a single directory using an  DFS approach.
     * Returns a PriorityQueue (Min-Heap) containing up to N largest files.
     */
    private fun scanDirectory(dir: File, n: Int, onFileScanned: () -> Unit): PriorityQueue<FileItem> {
        // Min-Heap: Keeps the smallest element at the top.
        // This guarantees O(N) memory usage instead of storing millions of files in memory.
        val minHeap = PriorityQueue<FileItem>(n, compareBy { it.size })

        // Iterative stack avoids StackOverflowError on deeply nested folders
        val stack = Stack<File>()
        stack.push(dir)

        while (stack.isNotEmpty()) {
            val currentDir = stack.pop()

            val files = try {
                currentDir.listFiles()
            } catch (e: SecurityException) {
                // In real Android devices, system folders throw SecurityExceptions. We skip them.
                Log.w(TAG, "Permission denied for: ${currentDir.path}")
                null
            }

            if (files == null) continue

            for (file in files) {
                if (file.isDirectory) {
                    // Ignore hidden folders to save time
                    if (!file.isHidden) stack.push(file)
                } else {
                    onFileScanned()

                    val size = file.length()
                    if (minHeap.size < n) {
                        minHeap.add(FileItem(file, size))
                    } else if (size > (minHeap.peek()?.size ?: 0L)) {
                        minHeap.poll() // Remove smallest
                        minHeap.add(FileItem(file, size)) // Add new larger file
                    }
                }
            }
        }
        return minHeap
    }
}