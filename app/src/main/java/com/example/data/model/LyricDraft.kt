package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.data.model.CategoryFilterType

/**
 * Room Entity representing an auto-saved draft of lyrics generation and studio state.
 */
@Entity(tableName = "drafts")
data class LyricDraft(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val genre: String,
    val mood: String,
    val language: String,
    val userTopic: String,
    val lyrics: String,
    val promptText: String,
    val tags: String,
    val coverConcept: String = "",
    val coverImagePath: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Returns the project status from the draft's tags (e.g. "status:IN PROGRESS").
 * Returns "DRAFT" if no status tag is found.
 */
fun LyricDraft.projectStatusFromTags(tags: String): String {
    val statusTag = tags.split(",")
        .map { it.trim() }
        .firstOrNull { it.startsWith("status:", ignoreCase = true) }
    return statusTag?.substringAfter(":") ?: "DRAFT"
}

/**
 * Checks if the draft matches the current filter criteria.
 */
fun LyricDraft.matchesFilter(
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
        CategoryFilterType.FAVORITES -> false
        CategoryFilterType.DRAFTS -> true
    }
}
