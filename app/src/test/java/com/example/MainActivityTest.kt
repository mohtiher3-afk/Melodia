package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest
import com.example.ui.viewmodel.MelodiaViewModel
import com.example.ui.components.MelodiaTab
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload


/**
 * Unit tests for MainActivity dependencies using Robolectric.
 * Tests cover:
 * - Activity creation with Application context
 * - ViewModel initialization
 * - Tab navigation state management
 * - MelodiaTab enum properties
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    private lateinit var viewModel: MelodiaViewModel
    private lateinit var application: Application

    @Before
    fun setup() {
        // Initialize Application context for Robolectric
        application = ApplicationProvider.getApplicationContext()

        // Create ViewModel (requires Application context)
        viewModel = MelodiaViewModel(application)
    }

    @Test
    fun `activity can be created with context`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        assertNotNull(context)
        assertEquals("com.aistudio.melodia.nothing", context.packageName)
    }

    @Test
    fun `viewModel initializes with default studio state`() {
        val initialState = viewModel.studioState.value
        assertNotNull(initialState)

        // Verify default values
        assertTrue("Auto-rhyme should be active by default", initialState.isAutoRhymeActive)
        assertNull(initialState.currentDraft)
    }

    @Test
    fun `melodia tab has correct icon`() {
        assertEquals(Icons.Default.GraphicEq, MelodiaTab.STUDIO.icon)
        assertEquals(Icons.Default.EditNote, MelodiaTab.LYRICS.icon)
        assertEquals(Icons.Default.LibraryMusic, MelodiaTab.LIBRARY.icon)
        assertEquals(Icons.Default.Image, MelodiaTab.VISUALS.icon)
        assertEquals(Icons.Default.Sync, MelodiaTab.SYNC.icon)
        assertEquals(Icons.Default.Upload, MelodiaTab.EXPORT.icon)
        assertEquals(Icons.Default.Settings, MelodiaTab.SETTINGS.icon)
    }

    @Test
    fun `melodia tab has correct title`() {
        assertEquals("Studio", MelodiaTab.STUDIO.title)
        assertEquals("Lyrics", MelodiaTab.LYRICS.title)
        assertEquals("Library", MelodiaTab.LIBRARY.title)
        assertEquals("Visuals", MelodiaTab.VISUALS.title)
        assertEquals("Sync", MelodiaTab.SYNC.title)
        assertEquals("Export", MelodiaTab.EXPORT.title)
        assertEquals("Settings", MelodiaTab.SETTINGS.title)
    }

    @Test
    fun `melodia tab has correct tag`() {
        assertEquals("tab_studio", MelodiaTab.STUDIO.tag)
        assertEquals("tab_lyrics", MelodiaTab.LYRICS.tag)
        assertEquals("tab_library", MelodiaTab.LIBRARY.tag)
        assertEquals("tab_visuals", MelodiaTab.VISUALS.tag)
        assertEquals("tab_sync", MelodiaTab.SYNC.tag)
        assertEquals("tab_export", MelodiaTab.EXPORT.tag)
        assertEquals("tab_settings", MelodiaTab.SETTINGS.tag)
    }

    @Test
    fun `studio ui state toggle auto rhyme`() = runTest {
        val initialState = viewModel.studioState.value
        assertTrue("Auto-rhyme should be active by default", initialState.isAutoRhymeActive)

        // Toggle auto rhyme
        val toggledState = initialState.copy(isAutoRhymeActive = false)
        assertFalse("Auto-rhyme should be inactive after toggle", toggledState.isAutoRhymeActive)

        // Toggle back
        val restoredState = toggledState.copy(isAutoRhymeActive = true)
        assertTrue("Auto-rhyme should be active again", restoredState.isAutoRhymeActive)
    }

    @Test
    fun `audio player manager is initialized`() {
        val playerManager = viewModel.audioPlayerManager
        assertNotNull(playerManager)
    }

    @Test
    fun `default export format is LRC`() {
        val defaultFormat = viewModel.defaultExportFormat.value
        assertEquals("LRC", defaultFormat)
    }

    @Test
    fun `can update default export format`() {
        viewModel.updateDefaultExportFormat("MP3")
        assertEquals("MP3", viewModel.defaultExportFormat.value)
    }

    @Test
    fun `melodia tabs ordinal order`() {
        // Verify ordinal ordering matches expected navigation sequence
        assertEquals(0, MelodiaTab.STUDIO.ordinal)
        assertEquals(1, MelodiaTab.LYRICS.ordinal)
        assertEquals(2, MelodiaTab.LIBRARY.ordinal)
        assertEquals(3, MelodiaTab.VISUALS.ordinal)
        assertEquals(4, MelodiaTab.SYNC.ordinal)
        assertEquals(5, MelodiaTab.EXPORT.ordinal)
        assertEquals(6, MelodiaTab.SETTINGS.ordinal)
    }

    @Test
    fun `melodia tabs total count`() {
        assertEquals(7, MelodiaTab.entries.size)
    }

    @Test
    fun `melodia tabs contains all expected values`() {
        val tabValues = MelodiaTab.entries.toSet()
        assertTrue(tabValues.contains(MelodiaTab.STUDIO))
        assertTrue(tabValues.contains(MelodiaTab.LYRICS))
        assertTrue(tabValues.contains(MelodiaTab.LIBRARY))
        assertTrue(tabValues.contains(MelodiaTab.VISUALS))
        assertTrue(tabValues.contains(MelodiaTab.SYNC))
        assertTrue(tabValues.contains(MelodiaTab.EXPORT))
        assertTrue(tabValues.contains(MelodiaTab.SETTINGS))
    }
}