package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RecentSubGenre
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSubGenreDao {
    /**
     * Retrieves the last three selected subgenres ordered by most recent selection timestamp.
     */
    @Query("SELECT * FROM recent_subgenres ORDER BY selectedAt DESC LIMIT 3")
    fun getRecentSubGenres(): Flow<List<RecentSubGenre>>

    /**
     * Inserts or replaces a subgenre, updating its selection timestamp.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecentSubGenre(subGenre: RecentSubGenre)

    /**
     * Deletes a specific subgenre by name from history.
     */
    @Query("DELETE FROM recent_subgenres WHERE name = :name")
    suspend fun deleteRecentSubGenre(name: String)

    /**
     * Clears all recent subgenres.
     */
    @Query("DELETE FROM recent_subgenres")
    suspend fun clearAllRecentSubGenres()
}
