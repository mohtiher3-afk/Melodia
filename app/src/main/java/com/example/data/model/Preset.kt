package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a saved prompt preset.
 */
@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val genre: String,
    val mood: String,
    val language: String,
    val vocalStyle: String,
    val bpm: String,
    val reverb: String,
    val createdAt: Long = System.currentTimeMillis()
)
