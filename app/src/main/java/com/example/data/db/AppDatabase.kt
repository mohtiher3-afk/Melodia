package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GeneratedLyrics
import com.example.data.model.GenerationQueueItem
import com.example.data.model.LyricDraft
import com.example.data.model.Song
import com.example.data.model.Preset
import com.example.data.model.RecentSubGenre

@Database(entities = [Song::class, LyricDraft::class, Preset::class, GeneratedLyrics::class, RecentSubGenre::class, GenerationQueueItem::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricDraftDao(): LyricDraftDao
    abstract fun presetDao(): PresetDao
    abstract fun generatedLyricsDao(): GeneratedLyricsDao
    abstract fun recentSubGenreDao(): RecentSubGenreDao
    abstract fun generationQueueDao(): GenerationQueueDao

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
                    .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 8)
                    // Handle downgrades (device DB version > app version) by dropping & recreating.
                    // Without this, a rollback from v8 to v7 crashes with
                    // "A migration from 8 to 7 was required but not found".
                    // The single-arg overload defaults to dumpAllTables=true, so ALL
                    // tables are dropped & recreated on downgrade. Use the 2-arg overload
                    // with dumpAllTables=false to drop only tables absent from the new
                    // schema, preserving data that still exists in the rolled-back schema.
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
