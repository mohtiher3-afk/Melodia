package com.example.data.repository

import com.example.data.db.PresetDao
import com.example.data.model.Preset
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets()

    suspend fun savePreset(preset: Preset): Long {
        return presetDao.insertPreset(preset)
    }

    suspend fun deletePreset(preset: Preset) {
        presetDao.deletePreset(preset)
    }

    suspend fun deletePresetById(id: Long) {
        presetDao.deletePresetById(id)
    }
}
