package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Melodia", appName)
  }

  @Test
  fun `test lyric draft auto save and retrieval in room`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.db.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.lyricDraftDao()
    val draft = com.example.data.model.LyricDraft(
      title = "Midnight Echoes",
      genre = "Synthwave",
      mood = "Late Night",
      language = "Bilingual",
      userTopic = "Neon streets and memories",
      lyrics = "[Verse 1]\nNeon signs illuminate the rain...",
      promptText = "Synthwave, 80s analog synth",
      tags = "synth, neon, retro"
    )

    val id = dao.insertDraft(draft)
    val fetched = dao.getLatestDraftSync()
    org.junit.Assert.assertNotNull(fetched)
    assertEquals("Midnight Echoes", fetched?.title)
    assertEquals("Synthwave", fetched?.genre)
    assertEquals("[Verse 1]\nNeon signs illuminate the rain...", fetched?.lyrics)

    dao.deleteDraftById(id)
    val afterDelete = dao.getLatestDraftSync()
    org.junit.Assert.assertNull(afterDelete)
    db.close()
  }



  @Test
  fun `test auto rhyme feature state toggle`() {
    val initialState = com.example.ui.viewmodel.StudioUiState()
    org.junit.Assert.assertTrue("Auto-rhyme should be active by default", initialState.isAutoRhymeActive)
    
    val toggledState = initialState.copy(isAutoRhymeActive = !initialState.isAutoRhymeActive)
    org.junit.Assert.assertFalse("Auto-rhyme should be inactive after toggle", toggledState.isAutoRhymeActive)
    
    val toggledBack = toggledState.copy(isAutoRhymeActive = !toggledState.isAutoRhymeActive)
    org.junit.Assert.assertTrue("Auto-rhyme should be active again", toggledBack.isAutoRhymeActive)
  }
}
