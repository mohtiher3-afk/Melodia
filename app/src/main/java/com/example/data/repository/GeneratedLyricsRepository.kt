package com.example.data.repository

import com.example.data.db.GeneratedLyricsDao
import com.example.data.model.GeneratedLyrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository to abstract data access to the GeneratedLyrics DAO,
 * following Clean Architecture and modern Android Room patterns.
 */
class GeneratedLyricsRepository(private val dao: GeneratedLyricsDao) {
    val allLyrics: Flow<List<GeneratedLyrics>> = dao.getAllLyrics()
    val favoriteLyrics: Flow<List<GeneratedLyrics>> = dao.getFavoriteLyrics()

    fun searchLyrics(query: String): Flow<List<GeneratedLyrics>> = dao.searchLyrics(query)

    fun getRecentLyrics(limit: Int = 10): Flow<List<GeneratedLyrics>> = dao.getRecentLyrics(limit)

    suspend fun getLyricsById(id: Long): GeneratedLyrics? = dao.getLyricsById(id)

    suspend fun saveGeneratedLyrics(
        songTitle: String,
        lyrics: String,
        genre: String = "Modern Beats",
        mood: String = "Atmospheric",
        language: String = "English",
        tags: String = "",
        promptText: String = ""
    ): Long {
        val entity = GeneratedLyrics(
            songTitle = songTitle.ifBlank { "Untitled Creation" },
            lyrics = lyrics,
            timestamp = System.currentTimeMillis(),
            genre = genre,
            mood = mood,
            language = language,
            tags = tags,
            promptText = promptText
        )
        return dao.insertLyrics(entity)
    }

    suspend fun insertLyrics(lyrics: GeneratedLyrics): Long = dao.insertLyrics(lyrics)

    suspend fun updateLyrics(lyrics: GeneratedLyrics) = dao.updateLyrics(lyrics)

    suspend fun deleteLyrics(lyrics: GeneratedLyrics) = dao.deleteLyrics(lyrics)

    suspend fun deleteLyricsById(id: Long) = dao.deleteLyricsById(id)

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) =
        dao.updateFavorite(id, !currentFavorite)

    suspend fun clearAllLyrics() = dao.clearAllLyrics()
}
