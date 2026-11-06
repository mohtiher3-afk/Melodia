package com.example.data.repository

import com.example.data.db.RecentSubGenreDao
import com.example.data.model.RecentSubGenre
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting access to recently viewed / selected subgenres stored in Room.
 */
class RecentSubGenreRepository(private val dao: RecentSubGenreDao) {
    /**
     * Flow emitting the last three selected subgenres.
     */
    val recentSubGenres: Flow<List<RecentSubGenre>> = dao.getRecentSubGenres()

    /**
     * Records or updates a subgenre selection with current timestamp.
     */
    suspend fun recordSubGenreSelected(name: String, category: String, tempoRange: String = "") {
        dao.insertOrUpdateRecentSubGenre(
            RecentSubGenre(
                name = name,
                category = category,
                tempoRange = tempoRange,
                selectedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeRecentSubGenre(name: String) {
        dao.deleteRecentSubGenre(name)
    }

    suspend fun clearRecentSubGenres() {
        dao.clearAllRecentSubGenres()
    }
}
