package com.example.ui.nothing

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

private const val BG = 0xFF000000

private val sampleSteps = listOf(
    NtStep("Decode audio", NtStepState.DONE),
    NtStep("Find vocal regions", NtStepState.NOW),
    NtStep("Estimate word timing", NtStepState.TODO),
    NtStep("Review timing", NtStepState.TODO)
)

@Preview(name = "Studio", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Preview(name = "Studio AR", locale = "ar", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Preview(name = "Studio 200%", fontScale = 2f, showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtStudioPreview() = NtAppShell(0, {}, ntStepLabel(0), stepIndex = 0) {
    NtStudioContent(
        prompt = "Late-night drive through a quiet city, regret and neon",
        genres = listOf("Pop", "Melancholic", "Arabic + English"),
        isGenerating = true,
        sectionLabel = "[VERSE 1]",
        lines = listOf("Streetlights bleeding down the glass", "أضواء المدينة تهمس باسمي"),
        writingLine = "Every mile I run from",
        onGenerate = {}
    )
}

@Preview(name = "Lyrics", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtLyricsPreview() = NtAppShell(0, {}, ntStepLabel(1), stepIndex = 1) {
    NtLyricsContent(
        sections = listOf("VERSE 1", "CHORUS", "BRIDGE"),
        selectedSection = 0,
        onSelectSection = {},
        onAddSection = {},
        lines = listOf("Streetlights bleeding down the glass", "أضواء المدينة تهمس باسمي", "Every mile I run from the past"),
        rhymeWord = "past",
        rhymes = listOf("last", "fast", "glass"),
        rhymesLoading = true,
        onPlay = {},
        onNext = {}
    )
}

@Preview(name = "Visuals", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtVisualsPreview() = NtAppShell(0, {}, ntStepLabel(2), stepIndex = 2) {
    NtVisualsContent("NEO", listOf("MONO DOTS", "RED PULSE", "GRID"), 0, {}, progress = 0.48f, onGenerate = {})
}

@Preview(name = "Sync", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtSyncPreview() = NtAppShell(0, {}, ntStepLabel(3), stepIndex = 3) {
    NtSyncContent("night_drive.mp3", "3:12", "Finding vocal regions", 0.64f, sampleSteps, "Streetlights", "bleeding", "down the glass", {})
}

@Preview(name = "Export", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtExportPreview() = NtAppShell(0, {}, ntStepLabel(4), stepIndex = 4) {
    NtExportContent(
        artist = "NEO",
        previewBefore = "Streetlights",
        previewWord = "bleeding",
        progress = 0.35f,
        frameText = "Frame 1,260 / 3,600",
        timeText = "42 s of 120 s · 1080p",
        steps = listOf(
            NtStep("Render frames", NtStepState.NOW),
            NtStep("Add audio", NtStepState.TODO),
            NtStep("Save to gallery", NtStepState.TODO)
        ),
        showBackgroundNote = false,
        canShare = false,
        onCancel = {},
        onShare = {}
    )
}

@Preview(name = "Library", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtLibraryPreview() = NtAppShell(
    1, {}, "LIBRARY",
    bottomExtra = { NtMiniPlayer("Night Drive", 0.4f, true, {}) }
) {
    NtLibraryContent(
        songs = listOf(
            NtSong("Night Drive", "NEO · 3:12 · Synced"),
            NtSong("Addicted to Lying", "NEO · 2:48 · Needs sync"),
            NtSong("Untitled draft", "Lyrics only")
        ),
        playingIndex = 0,
        onPlay = {}
    )
}

@Preview(name = "Settings", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 844)
@Composable
fun NtSettingsPreview() = NtAppShell(2, {}, "SETTINGS") {
    NtSettingsContent(
        languages = listOf("EN", "عربي"),
        selectedLanguage = 0,
        onSelectLanguage = {},
        cloudTranscription = false,
        onCloudChange = {},
        haptics = true,
        onHapticsChange = {},
        qualities = listOf("1080P", "4K"),
        selectedQuality = 0,
        onSelectQuality = {},
        storageUsed = 0.38f
    )
}

@Preview(name = "Icons", showBackground = true, backgroundColor = BG, widthDp = 390, heightDp = 160)
@Composable
fun NtIconsPreview() = androidx.compose.foundation.layout.Row(
    androidx.compose.ui.Modifier.padding(16.dp),
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
) {
    DotIcon.values().take(6).forEach { DotMatrixIcon(it, null, iconSize = 40.dp, tint = Nt.White, animated = true) }
}