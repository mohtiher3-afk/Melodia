package com.example.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Proves the Room upgrade path preserves user data:
 *  - a v7 database with rows in every table
 *  - migrates to v8 through the explicit MIGRATION_7_8
 *  - all rows survive and Room validates the resulting schema
 *
 * With no destructive fallback configured, a missing migration would now throw
 * instead of silently wiping the database — so this test fails loudly, not quietly.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun countRows(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM `$table`").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }

    @Test
    fun migrate7To8_preservesAllUserData() {
        val dbName = "migration-test.db"

        // 1. Create a v7 database and seed user data in every table.
        helper.createDatabase(dbName, 7).use { db ->
            db.execSQL(
                "INSERT INTO songs (id, title, genre, mood, language, promptText, tags, lyrics, coverSeed, " +
                    "coverConcept, coverImagePath, trebloGenerationId, audioUrl, createdAt, isFavorite, isLocal) " +
                    "VALUES (1, 'Neon Night', 'Pop', 'Energetic', 'en', 'a synthwave anthem', 'synth', " +
                    "'[Verse] neon night', 'seed-1', 'neon skyline', NULL, NULL, NULL, 1000, 0, 1)"
            )
            db.execSQL(
                "INSERT INTO drafts (id, title, genre, mood, language, userTopic, lyrics, promptText, tags, " +
                    "coverConcept, coverImagePath, lastModified) " +
                    "VALUES (1, 'Untitled Draft', 'Pop', 'Energetic', 'en', 'synthwave night drive', " +
                    "'[Verse] neon night', 'a synthwave anthem', 'synth', 'neon skyline', NULL, 2000)"
            )
            db.execSQL(
                "INSERT INTO presets (id, name, genre, mood, language, vocalStyle, bpm, reverb, createdAt) " +
                    "VALUES (1, 'Synthwave Drive', 'Pop', 'Energetic', 'en', 'warm', '120', 0.3, 3000)"
            )
            db.execSQL(
                "INSERT INTO generated_lyrics (id, song_title, lyrics, timestamp, genre, mood, language, tags, prompt_text, is_favorite) " +
                    "VALUES (1, 'Neon Night', '[Verse] neon night', 4000, 'Pop', 'Energetic', 'en', 'synth', 'a synthwave anthem', 0)"
            )
            db.execSQL(
                "INSERT INTO recent_subgenres (name, category, tempoRange, selectedAt) " +
                    "VALUES ('Synthwave', 'Electronic', '100-130', 5000)"
            )
        }

        // 2. Run the registered migrations up to v8 (MIGRATION_7_8 runs here)
        //    and let Room validate the migrated schema against 8.json.
        helper.runMigrationsAndValidate(dbName, 8, true, AppDatabase.MIGRATION_7_8).use { migrated ->
            // 3. Verify every seeded row survived the migration.
            assertEquals(1, countRows(migrated, "songs"))
            assertEquals(1, countRows(migrated, "drafts"))
            assertEquals(1, countRows(migrated, "presets"))
            assertEquals(1, countRows(migrated, "generated_lyrics"))
            assertEquals(1, countRows(migrated, "recent_subgenres"))
        }

        // 4. Also open through the real production builder path (same migration
        //    registration as AppDatabase.getInstance) and confirm it opens cleanly.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()
        try {
            db.openHelper.writableDatabase.query("SELECT 1").use { it.moveToFirst() }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrationObject_targetsCorrectVersions() {
        assertEquals(7, AppDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, AppDatabase.MIGRATION_7_8.endVersion)
    }
}
