package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for persisting and retrieving songs and lyrics.
 */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<Song>

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
