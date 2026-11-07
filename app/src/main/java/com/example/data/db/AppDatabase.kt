package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GeneratedLyrics
import com.example.data.model.LyricDraft
import com.example.data.model.Song
import com.example.data.model.Preset
import com.example.data.model.RecentSubGenre

@Database(entities = [Song::class, LyricDraft::class, Preset::class, GeneratedLyrics::class, RecentSubGenre::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricDraftDao(): LyricDraftDao
    abstract fun presetDao(): PresetDao
    abstract fun generatedLyricsDao(): GeneratedLyricsDao
    abstract fun recentSubGenreDao(): RecentSubGenreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "melodia_database"
                )
                    // Only schemas from before the migration policy may be wiped. From version 7 on,
                    // every schema change needs an explicit Migration so saved lyrics are never lost.
                    .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
