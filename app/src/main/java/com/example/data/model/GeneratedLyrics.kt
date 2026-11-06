package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing stored generated lyrics, associating each creation
 * with its song title, generation timestamp, musical style metadata, and user notes
 * so users can revisit and reload their creations anytime.
 */
@Entity(tableName = "generated_lyrics")
data class GeneratedLyrics(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "song_title")
    val songTitle: String,
    @ColumnInfo(name = "lyrics")
    val lyrics: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "genre")
    val genre: String = "Modern Beats",
    @ColumnInfo(name = "mood")
    val mood: String = "Atmospheric",
    @ColumnInfo(name = "language")
    val language: String = "English",
    @ColumnInfo(name = "tags")
    val tags: String = "",
    @ColumnInfo(name = "prompt_text")
    val promptText: String = "",
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
)
