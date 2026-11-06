package com.example.data.model

import java.util.UUID

/**
 * Represents a user-created album grouping songs/lyrics with a common theme.
 * Nothing OS aesthetic: minimalist, monochrome with red accent.
 * 
 * This is a plain data class (not Room entity) for simplicity,
 * and can be persisted via SharedPreferences or a simple repository.
 */
data class Album(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    var coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var songCount: Int = 0,
    val lyricTheme: String = "",
    val genre: String = "Melodic",
    val mood: String = "Neutral"
) {
    // Derived display name for Nothing OS UI
    val displayName: String
        get() = if (title.isNotBlank()) title else "Untitled Album"

    // Increment song count
    fun incrementSongCount(): Album {
        return copy(songCount = songCount + 1)
    }

    // Decrement song count (ensure not negative)
    fun decrementSongCount(): Album {
        return copy(songCount = if (songCount > 0) songCount - 1 else 0)
    }

    // Check if album is empty
    val isEmpty: Boolean
        get() = songCount <= 0

    // Human-readable created date
    val formattedCreated: String
        get() {
            val date = java.util.Date(createdAt)
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return sdf.format(date)
        }
}