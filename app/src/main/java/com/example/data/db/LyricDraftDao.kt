package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LyricDraft
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricDraftDao {
    @Query("SELECT * FROM drafts ORDER BY lastModified DESC")
    fun getAllDrafts(): Flow<List<LyricDraft>>

    @Query("SELECT * FROM drafts ORDER BY lastModified DESC LIMIT 1")
    fun getLatestDraft(): Flow<LyricDraft?>

    @Query("SELECT * FROM drafts ORDER BY lastModified DESC LIMIT 1")
    suspend fun getLatestDraftSync(): LyricDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: LyricDraft): Long

    @Update
    suspend fun updateDraft(draft: LyricDraft)

    @Delete
    suspend fun deleteDraft(draft: LyricDraft)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Long)

    @Query("DELETE FROM drafts")
    suspend fun clearAllDrafts()
}
