package com.example.data.repository

import com.example.data.db.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    suspend fun getSongsByIds(ids: List<Long>): List<Song> = songDao.getSongsByIds(ids)

    suspend fun insertSong(song: Song): Long = songDao.insertSong(song)

    suspend fun updateSong(song: Song) = songDao.updateSong(song)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

    suspend fun deleteById(id: Long) = songDao.deleteById(id)
}
