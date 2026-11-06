package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a generated song, its Treblo prompt, full lyrics, and cover image path.
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val genre: String,
    val mood: String,
    val language: String,
    val promptText: String,
    val tags: String,
    val lyrics: String,
    val coverSeed: String,
    val coverConcept: String = "",
    val coverImagePath: String? = null,
    val trebloGenerationId: String? = null,
    val audioUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isLocal: Boolean = false
)

typealias SongEntity = Song

/**
 * Returns the project status from the song's tags (e.g. "status:IN PROGRESS").
 * Returns "DRAFT" if no status tag is found.
 */
fun Song.projectStatusFromTags(tags: String): String {
    val statusTag = tags.split(",")
        .map { it.trim() }
        .firstOrNull { it.startsWith("status:", ignoreCase = true) }
    return statusTag?.substringAfter(":") ?: "DRAFT"
}

/**
 * Checks if the song matches the current filter criteria.
 */
fun Song.matchesFilter(
    searchQuery: String,
    category: CategoryFilterType,
    subTag: String?
): Boolean {
    if (searchQuery.isNotBlank()) {
        val query = searchQuery.lowercase()
        if (!title.lowercase().contains(query) &&
            !genre.lowercase().contains(query) &&
            !mood.lowercase().contains(query) &&
            !tags.lowercase().contains(query) &&
            !lyrics.lowercase().contains(query)
        ) {
            return false
        }
    }
    return when (category) {
        CategoryFilterType.ALL -> true
        CategoryFilterType.STATUS -> subTag?.let { projectStatusFromTags(tags).equals(it, ignoreCase = true) } ?: true
        CategoryFilterType.GENRE -> subTag?.let { genre.equals(it, ignoreCase = true) } ?: true
        CategoryFilterType.MOOD -> subTag?.let { mood.equals(it, ignoreCase = true) } ?: true
        CategoryFilterType.FAVORITES -> isFavorite
        CategoryFilterType.DRAFTS -> false
    }
}

enum class CategoryFilterType(val label: String) {
    ALL("ALL"),
    STATUS("STATUS"),
    GENRE("GENRE"),
    MOOD("MOOD"),
    FAVORITES("FAVORITES"),
    DRAFTS("DRAFTS")
}
