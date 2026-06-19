# Large File Finder (Android)

An highly optimized Android application designed to find the top **N largest files** within user-defined directories and subdirectories. This project was developed with a strong focus on algorithmic efficiency, memory management, and modern Android architecture.

## 🚀 Features

* **Dynamic Directory Selection:** Users can add multiple local directories to scan using a custom, high-speed built-in file picker.
* **Top N Files:** Easily specify how many of the largest files (N) to retrieve.
* **Real-time Progress:** Live scanning progress (files scanned) is updated dynamically in the UI.
* **System Notifications:** A persistent low-priority system notification updates the user on the search progress in the background.
* **Parallel Execution:** If multiple directories are selected (e.g., Internal Storage and External SD Card), the application automatically scans them concurrently on separate threads.
* **Modern UI:** Built entirely with Jetpack Compose (Material Design 3).

---

## 🧠 Architecture & Algorithmic Efficiency

This application is specifically engineered to handle massive file systems without crashing or running out of memory. 

### 1. O(N) Memory Optimization (Min-Heap)
A naive approach of loading all scanned files into a list and sorting them `O(M log M)` can easily cause an `OutOfMemoryError` on devices with hundreds of thousands of files. 
Instead, this app uses a **PriorityQueue (Min-Heap)** strictly bounded to size `N`. As we traverse the file system, we only keep the top N files in memory. If a newly discovered file is larger than the smallest file in our heap, we perform a swap. This ensures the memory footprint remains extremely lightweight and exactly `O(N)`.

### 2. StackOverflow Prevention (Iterative DFS)
Recursive directory traversal can trigger a `StackOverflowError` when encountering deeply nested directories (e.g., node_modules, deep caches). This app uses an **Iterative Depth-First Search** utilizing a `java.util.Stack`, shifting the memory load from the limited thread call-stack to the JVM heap.

### 3. Parallel Disk Scanning
To satisfy the requirement of parallel search across different physical media, the app utilizes **Kotlin Coroutines (`Dispatchers.IO`)**. Every root directory selected by the user launches its own `async` block. The system scans these directories completely in parallel, generates a local Min-Heap for each, and mathematically merges them into a final top N result.

### 4. Lightning-Fast File I/O
Instead of using the Android Storage Access Framework (SAF) — which introduces heavy IPC overhead and is notoriously slow for batch metadata reading — this app utilizes the raw `java.io.File` API. This allows the application to read hundreds of thousands of files in mere milliseconds.

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Concurrency:** Kotlin Coroutines & Flow (`StateFlow`)
* **Data Structures:** `PriorityQueue` (Min-Heap), `Stack` (Iterative DFS)
* **Minimum SDK:** 21 (Android 5.0)
* **Target SDK:** 34 (Android 14)

