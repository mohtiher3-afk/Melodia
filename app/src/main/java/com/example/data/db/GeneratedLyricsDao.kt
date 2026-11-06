package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GeneratedLyrics
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for persisting, querying, and managing generated lyrics
 * associated with song titles and timestamps.
 */
@Dao
interface GeneratedLyricsDao {
    @Query("SELECT * FROM generated_lyrics ORDER BY timestamp DESC")
    fun getAllLyrics(): Flow<List<GeneratedLyrics>>

    @Query("SELECT * FROM generated_lyrics WHERE id = :id LIMIT 1")
    suspend fun getLyricsById(id: Long): GeneratedLyrics?

    @Query("SELECT * FROM generated_lyrics WHERE song_title LIKE '%' || :query || '%' OR lyrics LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLyrics(query: String): Flow<List<GeneratedLyrics>>

    @Query("SELECT * FROM generated_lyrics WHERE is_favorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteLyrics(): Flow<List<GeneratedLyrics>>

    @Query("SELECT * FROM generated_lyrics ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLyrics(limit: Int): Flow<List<GeneratedLyrics>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: GeneratedLyrics): Long

    @Update
    suspend fun updateLyrics(lyrics: GeneratedLyrics)

    @Delete
    suspend fun deleteLyrics(lyrics: GeneratedLyrics)

    @Query("DELETE FROM generated_lyrics WHERE id = :id")
    suspend fun deleteLyricsById(id: Long)

    @Query("UPDATE generated_lyrics SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM generated_lyrics")
    suspend fun clearAllLyrics()
}
