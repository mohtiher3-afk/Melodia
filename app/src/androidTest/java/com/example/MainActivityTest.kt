package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        composeTestRule.onNodeWithText("Studio").assertExists()
        composeTestRule.onNodeWithText("Library").assertExists()
        composeTestRule.onNodeWithText("Visuals").assertExists()
        composeTestRule.onNodeWithText("Sync").assertExists()
        composeTestRule.onNodeWithText("Export").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun tabNavigationSwitchesContent() {
        composeTestRule.onNodeWithText("Library").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Library").assertExists()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun floatingPlayerBarIsHiddenWhenNoAudio() {
        composeTestRule.onNodeWithTag("floating_player_bar").assertDoesNotExist()
    }

    @Test
    fun sentimentAnalyzeChipExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ANALYZE SENTIMENT").performScrollTo().assertExists()
    }

    @Test
    fun saveAsPresetButtonExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SAVE AS PRESET").performScrollTo().assertExists()
    }

    @Test
    fun settingsScreenHasContent() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun settingsScreenRenders() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Melodia Studio Settings").assertExists()
    }

    @Test
    fun lyricsRhymeButtonExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("RHYME").assertExists()
    }

    @Test
    fun topicInputAcceptsText() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("topic_input").performScrollTo().performTextInput("test topic")
        composeTestRule.onNodeWithText("test topic").assertExists()
    }

    @Test
    fun genreDropdownIsInteractive() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("genre_dropdown_box").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("genre_dropdown_box").assertExists()
    }

    @Test
    fun moodKeywordsInputWorks() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mood_keywords_input").performScrollTo().performTextInput("cyberpunk")
        composeTestRule.onNodeWithText("cyberpunk").assertExists()
    }

    @Test
    fun studioScreenRenders() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("studio_screen").assertExists()
    }

    @Test
    fun lyricsEditorIsEditable() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lyrics_editor").performTextClearance()
        composeTestRule.onNodeWithTag("lyrics_editor").performTextInput("hello world")
        composeTestRule.onNodeWithText("hello world").assertExists()
    }

    @Test
    fun lyricsDictateButtonExists() {
        composeTestRule.onNodeWithText("Studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Lyrics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DICTATE").assertExists()
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