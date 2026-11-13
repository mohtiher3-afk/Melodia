package com.example.ui.screens.studio

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.viewmodel.StudioUiState

private const val BG = 0xFF000000

@Preview(name = "Config Section", showBackground = true, backgroundColor = BG, widthDp = 390)
@Composable
private fun StudioConfigSectionPreview() {
    StudioConfigSection(
        state = StudioUiState(
            selectedGenre = "Synthwave",
            selectedLanguage = "Bilingual",
            moodKeywords = "Cyberpunk, late-night melancholy, neon",
            vocalStyle = "Auto-Tuned Male",
            selectedBpm = "120 BPM",
            vocalReverb = "Studio Clean"
        ),
        onSetGenre = {},
        onSetLanguage = {},
        onSetMoodKeywords = {},
        onSetVocalStyle = {},
        onSetSelectedBpm = {},
        onSetVocalReverb = {},
        onOpenGenreDirectory = {}
    )
}

@Preview(name = "Config AR", locale = "ar", showBackground = true, backgroundColor = BG, widthDp = 390)
@Composable
private fun StudioConfigSectionArabicPreview() {
    StudioConfigSection(
        state = StudioUiState(
            selectedGenre = "Arabic Pop",
            selectedLanguage = "Arabic",
            moodKeywords = "طابع ليلي، حنين",
            vocalStyle = "Wahba Male",
            selectedBpm = "100 BPM",
            vocalReverb = "Wide Hall"
        ),
        onSetGenre = {},
        onSetLanguage = {},
        onSetMoodKeywords = {},
        onSetVocalStyle = {},
        onSetSelectedBpm = {},
        onSetVocalReverb = {},
        onOpenGenreDirectory = {}
    )
}

@Preview(name = "Config 200%", fontScale = 2f, showBackground = true, backgroundColor = BG, widthDp = 390)
@Composable
private fun StudioConfigSectionLargeFontPreview() {
    StudioConfigSection(
        state = StudioUiState(
            selectedGenre = "Classical Tarab",
            selectedLanguage = "English",
            moodKeywords = "Nostalgic",
            vocalStyle = "Female Soul",
            selectedBpm = "140 BPM",
            vocalReverb = "Cathedral"
        ),
        onSetGenre = {},
        onSetLanguage = {},
        onSetMoodKeywords = {},
        onSetVocalStyle = {},
        onSetSelectedBpm = {},
        onSetVocalReverb = {},
        onOpenGenreDirectory = {}
    )
}
