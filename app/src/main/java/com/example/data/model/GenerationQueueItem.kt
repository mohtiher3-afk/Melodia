package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a queued generation task.
 */
@Entity(tableName = "generation_queue")
data class GenerationQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: GenerationType,
    val status: GenerationStatus = GenerationStatus.PENDING,
    val title: String,
    val genre: String,
    val mood: String,
    val language: String,
    val userTopic: String,
    val lyrics: String = "",
    val promptText: String = "",
    val tags: String = "",
    val coverConcept: String = "",
    val coverImagePath: String? = null,
    val vocalStyle: String = "Auto-Tuned Male",
    val selectedBpm: String = "120 BPM",
    val vocalReverb: String = "Studio Clean",
    val isInstrumental: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val errorMessage: String? = null,
    val generatedAudioPath: String? = null,
    val generatedCoverPath: String? = null
)

enum class GenerationType(val displayName: String) {
    LYRICS("Lyrics"),
    MUSIC("Music"),
    COVER("Cover Art"),
    FULL_SONG("Full Song")
}

enum class GenerationStatus(val displayName: String, val isActive: Boolean) {
    PENDING("Pending", true),
    PROCESSING("Processing", true),
    COMPLETED("Completed", false),
    FAILED("Failed", false),
    CANCELLED("Cancelled", false)
}