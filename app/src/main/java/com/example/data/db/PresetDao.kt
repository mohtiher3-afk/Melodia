package com.example.data.db

import androidx.room.*
import com.example.data.model.Preset
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}
