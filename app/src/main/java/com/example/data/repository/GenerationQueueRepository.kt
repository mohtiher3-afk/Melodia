package com.example.data.repository

import com.example.data.db.GenerationQueueDao
import com.example.data.model.GenerationQueueItem
import com.example.data.model.GenerationStatus
import kotlinx.coroutines.flow.Flow

class GenerationQueueRepository(private val generationQueueDao: GenerationQueueDao) {
    val allQueueItems: Flow<List<GenerationQueueItem>> = generationQueueDao.getAllQueueItems()
    val activeQueueItems: Flow<List<GenerationQueueItem>> = generationQueueDao.getActiveQueueItems()
    val completedQueueItems: Flow<List<GenerationQueueItem>> = generationQueueDao.getCompletedQueueItems()
    val failedQueueItems: Flow<List<GenerationQueueItem>> = generationQueueDao.getFailedQueueItems()

    suspend fun getQueueItemById(id: Long): GenerationQueueItem? = generationQueueDao.getQueueItemById(id)

    suspend fun enqueueItem(item: GenerationQueueItem): Long = generationQueueDao.insertQueueItem(item)

    suspend fun enqueueItems(items: List<GenerationQueueItem>) = generationQueueDao.insertQueueItems(items)

    suspend fun updateQueueItem(item: GenerationQueueItem) = generationQueueDao.updateQueueItem(item)

    suspend fun removeQueueItem(item: GenerationQueueItem) = generationQueueDao.deleteQueueItem(item)

    suspend fun removeQueueItemById(id: Long) = generationQueueDao.deleteQueueItemById(id)

    suspend fun clearCompletedItems() = generationQueueDao.clearCompletedItems()

    suspend fun clearAllQueueItems() = generationQueueDao.clearAllQueueItems()

    suspend fun updateStatus(id: Long, status: String, startedAt: Long) = generationQueueDao.updateStatus(id, status, startedAt)

    suspend fun completeItem(id: Long, status: String, completedAt: Long, audioPath: String?, coverPath: String?, error: String?) =
        generationQueueDao.completeItem(id, status, completedAt, audioPath, coverPath, error)

    suspend fun getActiveCount(): Int = generationQueueDao.getActiveCount()
}