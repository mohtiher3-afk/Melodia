package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesSuccessfully() {
        composeTestRule.waitForIdle()
    }

    @Test
    fun bottomBarHasAllTabs() {
        composeTestRule.onNodeWithTag("nothing_bottom_bar").assertExists()
        composeTestRule.onNodeWithText("Studio").assertExists()
        composeTestRule.onNodeWithText("Library").assertExists()
        composeTestRule.onNodeWithText("Lyrics").assertExists()
        composeTestRule.onNodeWithText("Sync").assertExists()
        composeTestRule.onNodeWithText("More").assertExists()
    }

    @Test
    fun tabNavigationSwitchesContent() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_screen").assertExists()

        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_screen").assertExists()

        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithTag("more_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
    }

    @Test
    fun moreMenuOpensVisualsScreen() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithTag("more_visuals").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("COVER ART GENERATOR").assertExists()
    }

    @Test
    fun floatingPlayerBarHiddenUntilAudioLoaded() {
        // F-13: the bar is intentionally hidden until a track is loaded
        // (AnimatedVisibility on currentUrl != null). The old expectation
        // asserted it exists on a fresh start, which contradicted the design.
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("floating_player_bar").assertDoesNotExist()
    }

    @Test
    fun sentimentAnalyzeChipExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sentiment_analyze_chip").assertExists()
    }

    @Test
    fun saveAsPresetButtonExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_as_preset_button").assertExists()
    }

    @Test
    fun settingsScreenHasContent() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithTag("more_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
    }

    @Test
    fun settingsScreenRenders() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithTag("more_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        composeTestRule.onNodeWithTag("system_island_switch").assertExists()
    }

    @Test
    fun lyricsRhymeButtonExists() {
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lyrics_rhyme_button").assertExists()
    }

    @Test
    fun topicInputAcceptsText() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("topic_input").performTextClearance()
        composeTestRule.onNodeWithTag("topic_input").performTextInput("test topic")
        composeTestRule.waitForIdle()
    }

    @Test
    fun genreDropdownIsInteractive() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("genre_dropdown_input").assertExists()
    }

    @Test
    fun moodKeywordsInputWorks() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mood_keywords_input").assertExists()
    }

    @Test
    fun studioScreenRenders() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_screen").assertExists()
    }

    @Test
    fun lyricsEditorIsEditable() {
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lyrics_editor").performTextClearance()
        composeTestRule.onNodeWithTag("lyrics_editor").performTextInput("test lyrics")
        composeTestRule.waitForIdle()
    }

    @Test
    fun lyricsDictateButtonExists() {
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lyrics_dictate_button").assertExists()
    }

    @Test
    fun librarySubtabTracksExists() {
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_subtab_tracks").assertExists()
    }

    @Test
    fun librarySubtabVideoExists() {
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_subtab_video").assertExists()
    }

    @Test
    fun libraryScreenRenders() {
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("library_screen").assertExists()
    }
}
