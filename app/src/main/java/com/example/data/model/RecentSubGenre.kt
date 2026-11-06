package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity tracking recently viewed / selected subgenres.
 * Uses the subgenre name as PrimaryKey so selecting it repeatedly
 * updates its timestamp and avoids duplicates.
 */
@Entity(tableName = "recent_subgenres")
data class RecentSubGenre(
    @PrimaryKey
    val name: String,
    val category: String,
    val tempoRange: String = "",
    val selectedAt: Long = System.currentTimeMillis()
)
