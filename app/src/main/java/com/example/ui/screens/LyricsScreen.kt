package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlyphCard
import com.example.ui.components.NothingPillButton
import com.example.ui.dialogs.RhymeDialog
import com.example.ui.dialogs.VoiceDictationDialog
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel

private val SECTION_TAGS = listOf(
    "Intro", "Verse 1", "Pre-Chorus", "Chorus", "Verse 2", "Bridge", "Outro"
)

@Composable
fun LyricsScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val externalLyrics = state.currentDraft?.lyrics ?: ""

    // The editor keeps its own text so typing never round-trips through the
    // ViewModel (which would reset the cursor and reformat the lyrics on every
    // keystroke). External changes (new generation, restored draft) are synced in.
    var editorText by rememberSaveable { mutableStateOf(externalLyrics) }
    var lastSynced by rememberSaveable { mutableStateOf(externalLyrics) }

    LaunchedEffect(externalLyrics) {
        if (externalLyrics != lastSynced) {
            editorText = externalLyrics
            lastSynced = externalLyrics
        }
    }

    fun applyLyrics(updated: String) {
        editorText = updated
        lastSynced = updated
        viewModel.updateLyrics(updated)
    }

    fun insertSectionTag(tag: String) {
        val block = "[$tag]"
        if (editorText.contains(block, ignoreCase = true)) {
            viewModel.updateLyrics(editorText)
            return
        }
        val trimmed = editorText.trimEnd()
        applyLyrics(if (trimmed.isEmpty()) "$block\n" else "$trimmed\n\n$block\n")
    }

    val wordCount = editorText.split(Regex("\\s+")).count { it.isNotBlank() }
    val lineCount = editorText.lines().count { it.isNotBlank() }
    val sectionCount = SECTION_TAGS.count { editorText.contains("[$it]", ignoreCase = true) }
    // Rough sung-pace estimate: ~2.2 words per second at a mid-tempo delivery.
    val estimatedSeconds = if (wordCount == 0) 0 else (wordCount / 2.2f).toInt()
    val rhymeQuery = editorText.lines().lastOrNull { it.isNotBlank() }?.trim().orEmpty()

    var showDictationDialog by rememberSaveable { mutableStateOf(false) }
    var showRhymeDialog by rememberSaveable { mutableStateOf(false) }

    fun insertWord(word: String) {
        val trimmed = editorText.trimEnd()
        applyLyrics(if (trimmed.isEmpty()) word else "$trimmed $word")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlyphCard(
            headerTitle = "STRUCTURED LYRICS EDITOR",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.currentDraft?.title?.takeIf { it.isNotBlank() } ?: "UNTITLED DRAFT",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$lineCount LINES // $wordCount WORDS",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingGrey
                        )
                        Text(
                            text = "~${formatDuration(estimatedSeconds)} // $sectionCount SECTIONS",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 9.sp,
                            color = NothingRed
                        )
                    }
                }

                // Writing tools: dictation + rhyme engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingPillButton(
                        text = "DICTATE",
                        onClick = { showDictationDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lyrics_dictate_button")
                    )
                    NothingPillButton(
                        text = "RHYME",
                        onClick = { showRhymeDialog = true },
                        enabled = rhymeQuery.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lyrics_rhyme_button")
                    )
                }

                // Quick section-tag insertion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SECTION_TAGS.forEach { tag ->
                        NothingPillButton(
                            text = tag,
                            onClick = { insertSectionTag(tag) }
                        )
                    }
                }

                OutlinedTextField(
                    value = editorText,
                    onValueChange = { applyLyrics(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("lyrics_editor"),
                    placeholder = {
                        Text(
                            text = "Write or paste your lyrics here...\nUse [Verse 1], [Chorus], [Bridge] tags to structure the song.",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 12.sp,
                            color = NothingGrey
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 14.sp,
                        color = NothingWhite
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite,
                        cursorColor = NothingRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }

    if (showDictationDialog) {
        VoiceDictationDialog(
            onTextDictated = { dictated ->
                if (dictated.isNotBlank()) insertWord(dictated.trim())
            },
            onDismiss = { showDictationDialog = false }
        )
    }

    if (showRhymeDialog) {
        RhymeDialog(
            query = rhymeQuery,
            onInsert = { word -> insertWord(word) },
            onDismiss = { showRhymeDialog = false }
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%d:%02d", safe / 60, safe % 60)
}
