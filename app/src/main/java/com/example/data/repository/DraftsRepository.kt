package com.example.data.repository

import com.example.data.db.LyricDraftDao
import com.example.data.model.LyricDraft
import kotlinx.coroutines.flow.Flow

class DraftsRepository(private val lyricDraftDao: LyricDraftDao) {
    val allDrafts: Flow<List<LyricDraft>> = lyricDraftDao.getAllDrafts()
    val latestDraft: Flow<LyricDraft?> = lyricDraftDao.getLatestDraft()

    suspend fun getLatestDraftSync(): LyricDraft? = lyricDraftDao.getLatestDraftSync()

    suspend fun saveDraft(draft: LyricDraft): Long = lyricDraftDao.insertDraft(draft)

    suspend fun deleteDraft(draft: LyricDraft) = lyricDraftDao.deleteDraft(draft)

    suspend fun deleteDraftById(id: Long) = lyricDraftDao.deleteDraftById(id)

    suspend fun clearAllDrafts() = lyricDraftDao.clearAllDrafts()
}
