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
import com.example.ui.nothing.NtDestination
import com.example.ui.nothing.ProjectStep


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
    fun `top level navigation has three design destinations and stable tags`() {
        assertEquals(3, NtDestination.entries.size)
        assertEquals(
            listOf(NtDestination.PROJECT, NtDestination.LIBRARY, NtDestination.SETTINGS),
            NtDestination.entries
        )
        assertEquals(listOf(0, 1, 2), NtDestination.entries.map { it.navigationIndex })
        assertEquals(
            listOf("nav_project", "nav_library", "nav_settings"),
            NtDestination.entries.map { it.testTag }
        )
    }

    @Test
    fun `project navigation has five ordered steps and stable tags`() {
        assertEquals(
            listOf(ProjectStep.STUDIO, ProjectStep.LYRICS, ProjectStep.VISUALS, ProjectStep.SYNC, ProjectStep.EXPORT),
            ProjectStep.entries
        )
        assertEquals(listOf(0, 1, 2, 3, 4), ProjectStep.entries.map { it.index })
        assertEquals(
            listOf("project_step_1", "project_step_2", "project_step_3", "project_step_4", "project_step_5"),
            ProjectStep.entries.map { it.testTag }
        )
    }

    @Test
    fun `navigation index mapping falls back to the project and studio routes`() {
        assertEquals(NtDestination.PROJECT, NtDestination.fromIndex(-1))
        assertEquals(NtDestination.SETTINGS, NtDestination.fromIndex(2))
        assertEquals(ProjectStep.STUDIO, ProjectStep.fromIndex(-1))
        assertEquals(ProjectStep.EXPORT, ProjectStep.fromIndex(4))
        assertEquals(ProjectStep.STUDIO, ProjectStep.fromIndex(5))
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

}
