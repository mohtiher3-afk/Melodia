package com.example.domain

import android.content.Context
import java.io.File

object AudioCacheManager {

    /**
     * Calculates the total size of audio cache and generated media files in bytes.
     */
    fun calculateCacheSizeBytes(context: Context): Long {
        var total = 0L
        val dirs = listOf(
            context.cacheDir,
            File(context.filesDir, "covers")
        )

        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        total += file.length()
                    }
                }
            }
        }
        return total
    }

    /**
     * Formats bytes into human-readable string (e.g., "12.4 MB").
     */
    fun getFormattedCacheSize(context: Context): String {
        val bytes = calculateCacheSizeBytes(context)
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    /**
     * Clears cached audio files and temporary generation files.
     * Returns the total bytes deleted.
     */
    fun clearAudioCache(context: Context): Long {
        var deletedBytes = 0L
        val dirs = listOf(
            context.cacheDir
        )

        for (dir in dirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.endsWith(".wav") || file.name.endsWith(".mp3") || file.name.endsWith(".tmp"))) {
                        val len = file.length()
                        if (file.delete()) {
                            deletedBytes += len
                        }
                    }
                }
            }
        }
        return deletedBytes
    }

    /**
     * Automatic background trim if total cache size exceeds maximum limit.
     */
    fun trimCacheIfOverLimit(context: Context, maxBytes: Long = 100 * 1024 * 1024L) {
        val currentSize = calculateCacheSizeBytes(context)
        if (currentSize <= maxBytes) return

        val cacheFiles = mutableListOf<File>()
        context.cacheDir.listFiles()?.filter { it.isFile }?.let { cacheFiles.addAll(it) }

        // Sort by last modified time (oldest first)
        cacheFiles.sortBy { it.lastModified() }

        var freed = 0L
        val targetToFree = currentSize - (maxBytes * 0.7).toLong()

        for (file in cacheFiles) {
            if (freed >= targetToFree) break
            val len = file.length()
            if (file.delete()) {
                freed += len
            }
        }
    }
}
