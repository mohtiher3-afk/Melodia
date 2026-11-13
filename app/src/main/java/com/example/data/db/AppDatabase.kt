package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.GeneratedLyrics
import com.example.data.model.LyricDraft
import com.example.data.model.Preset
import com.example.data.model.RecentSubGenre
import com.example.data.model.Song

@Database(entities = [Song::class, LyricDraft::class, Preset::class, GeneratedLyrics::class, RecentSubGenre::class], version = 8, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricDraftDao(): LyricDraftDao
    abstract fun presetDao(): PresetDao
    abstract fun generatedLyricsDao(): GeneratedLyricsDao
    abstract fun recentSubGenreDao(): RecentSubGenreDao

    companion object {
        /**
         * v7 -> v8: schema unchanged; explicit no-op migration that exists to
         * establish the migration path and prove upgrades no longer destroy data.
         * Future schema changes must add a real MIGRATION_N_M here (never
         * fallbackToDestructiveMigration).
         */
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change between 7 and 8.
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "melodia_database"
                )
                    .addMigrations(MIGRATION_7_8)
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