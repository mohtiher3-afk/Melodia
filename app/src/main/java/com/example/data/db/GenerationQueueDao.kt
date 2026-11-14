package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GenerationQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationQueueDao {
    @Query("SELECT * FROM generation_queue ORDER BY createdAt ASC")
    fun getAllQueueItems(): Flow<List<GenerationQueueItem>>

    @Query("SELECT * FROM generation_queue WHERE status IN ('PENDING', 'PROCESSING') ORDER BY createdAt ASC")
    fun getActiveQueueItems(): Flow<List<GenerationQueueItem>>

    @Query("SELECT * FROM generation_queue WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedQueueItems(): Flow<List<GenerationQueueItem>>

    @Query("SELECT * FROM generation_queue WHERE status = 'FAILED' ORDER BY completedAt DESC")
    fun getFailedQueueItems(): Flow<List<GenerationQueueItem>>

    @Query("SELECT * FROM generation_queue WHERE id = :id")
    suspend fun getQueueItemById(id: Long): GenerationQueueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: GenerationQueueItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<GenerationQueueItem>)

    @Update
    suspend fun updateQueueItem(item: GenerationQueueItem)

    @Delete
    suspend fun deleteQueueItem(item: GenerationQueueItem)

    @Query("DELETE FROM generation_queue WHERE id = :id")
    suspend fun deleteQueueItemById(id: Long)

    @Query("DELETE FROM generation_queue WHERE status = 'COMPLETED' OR status = 'FAILED' OR status = 'CANCELLED'")
    suspend fun clearCompletedItems()

    @Query("DELETE FROM generation_queue")
    suspend fun clearAllQueueItems()

    @Query("UPDATE generation_queue SET status = :status, startedAt = :startedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, startedAt: Long)

    @Query("UPDATE generation_queue SET status = :status, completedAt = :completedAt, generatedAudioPath = :audioPath, generatedCoverPath = :coverPath, errorMessage = :error WHERE id = :id")
    suspend fun completeItem(id: Long, status: String, completedAt: Long, audioPath: String?, coverPath: String?, error: String?)

    @Query("SELECT COUNT(*) FROM generation_queue WHERE status IN ('PENDING', 'PROCESSING')")
    suspend fun getActiveCount(): Int
}