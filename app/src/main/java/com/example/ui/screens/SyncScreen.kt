package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LyricsExportManager
import com.example.ui.components.GlyphCard
import com.example.ui.components.KaraokeLyricsView
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.components.StatusBadge
import com.example.ui.dialogs.ManualSyncDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun SyncScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val allowCloudTranscription by viewModel.allowCloudTranscription.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val player = viewModel.audioPlayerManager
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val playbackPosition by player.currentPosition.collectAsStateWithLifecycle()
    val playbackDuration by player.duration.collectAsStateWithLifecycle()
    val playbackUrl by player.currentUrl.collectAsStateWithLifecycle()
    var showTranscribeConfirmation by remember { mutableStateOf(false) }
    var showManualSync by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLocalAudio(context, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val draft = state.currentDraft
        val karaoke = state.karaokeLyrics

        GlyphCard(headerTitle = "AUTOMATIC LYRICS & SYNC") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header with StatusBadge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sync_alignment_status),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )
                    StatusBadge(
                        status = when {
                            state.isTranscribingAudio -> "TRANSCRIBING"
                            state.isAligningAudio -> "PROCESSING"
                            state.karaokeLyrics != null -> "READY"
                            else -> "IDLE"
                        }
                    )
                }

                if (state.isLocalAudioActive && state.localAudioMetadata != null) {
                    Text(
                        text = stringResource(R.string.sync_imported_audio, state.localAudioMetadata!!.title),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingWhite,
                        fontWeight = FontWeight.Bold
                    )
                    val withinCloudLimit = com.example.domain.LocalAudioImporter
                        .isWithinTranscriptionLimit(state.localAudioMetadata!!.localFile)
                    val canTranscribe = withinCloudLimit && allowCloudTranscription
                    Text(
                        text = when {
                            !withinCloudLimit -> "Audio remains local; cloud transcription is limited to files up to 14 MB."
                            !allowCloudTranscription -> "Cloud transcription is off. Enable it in Settings to upload this audio; local Whisper is not integrated yet."
                            else -> "This audio may be sent to Gemini for transcription (up to 14 MB)."
                        },
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
                    )
                    NothingButton(
                        text = if (state.isTranscribingAudio) "TRANSCRIBING VOCALS..." else "EXTRACT LYRICS FROM AUDIO",
                        onClick = {
                            if (!draft?.lyrics.isNullOrBlank()) showTranscribeConfirmation = true
                            else viewModel.transcribeImportedAudio(context)
                        },
                        style = NothingButtonStyle.Primary,
                        enabled = canTranscribe && !state.isTranscribingAudio && !state.isImportingAudio,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sync_no_audio),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )
                    NothingButton(
                        text = stringResource(R.string.sync_import_audio),
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        style = NothingButtonStyle.Secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NothingButton(
                    text = if (state.isAligningAudio) "CREATING WORD TIMINGS..." else "AUTO-CREATE WORD TIMINGS",
                    onClick = { viewModel.syncLyricsWithAudio(context) },
                    style = NothingButtonStyle.Primary,
                    enabled = !state.isAligningAudio && !draft?.lyrics.isNullOrBlank() && state.isLocalAudioActive
                )

                Text(
                    text = stringResource(R.string.sync_timing_help),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingGrey
                )

                if (karaoke != null) {
                    Text(
                        text = stringResource(R.string.sync_word_timestamps, karaoke.wordTimings.size),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NothingButton(
                            text = stringResource(R.string.sync_share_lrc),
                            onClick = {
                                if (draft != null) {
                                    val song = com.example.data.model.Song(
                                        title = draft.title,
                                        genre = draft.genre,
                                        mood = draft.mood,
                                        language = draft.language,
                                        promptText = draft.promptText,
                                        tags = draft.tags.joinToString(", "),
                                        lyrics = draft.lyrics,
                                        coverSeed = "seed_${System.currentTimeMillis()}"
                                    )
                                    val intent = LyricsExportManager.createLrcExportIntent(context, song)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export LRC"))
                                }
                            },
                            style = NothingButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )

                        NothingButton(
                            text = stringResource(R.string.sync_share_json),
                            onClick = {
                                if (draft != null) {
                                    val song = com.example.data.model.Song(
                                        title = draft.title,
                                        genre = draft.genre,
                                        mood = draft.mood,
                                        language = draft.language,
                                        promptText = draft.promptText,
                                        tags = draft.tags.joinToString(", "),
                                        lyrics = draft.lyrics,
                                        coverSeed = "seed_${System.currentTimeMillis()}"
                                    )
                                    val intent = LyricsExportManager.createJsonExportIntent(context, song)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export JSON"))
                                }
                            },
                            style = NothingButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )

                        NothingButton(
                            text = "MANUAL SYNC",
                            onClick = { showManualSync = true },
                            style = NothingButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (karaoke != null) {
            GlyphCard(headerTitle = "KARAOKE PREVIEW") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KaraokeLyricsView(
                        karaokeLyrics = karaoke,
                        currentPositionMs = playbackPosition.toLong(),
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingButton(
                            text = when {
                                playbackUrl == null -> "IMPORT AUDIO TO PREVIEW"
                                isPlaying -> "PAUSE PREVIEW"
                                else -> "PLAY PREVIEW"
                            },
                            onClick = {
                                playbackUrl?.let { player.playAudio(it) }
                            },
                            style = if (playbackUrl != null) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
                            enabled = playbackUrl != null,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = stringResource(R.string.sync_position_clock, formatClock(playbackPosition), formatClock(playbackDuration)),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        if (showManualSync) {
            ManualSyncDialog(
                lyrics = karaoke?.lyrics ?: "",
                audioUrl = playbackUrl ?: "",
                songTitle = draft?.title ?: "Untitled",
                audioPlayerManager = player,
                onApplySyncedLyrics = { synced ->
                    viewModel.updateLyrics(synced)
                },
                onDismiss = { showManualSync = false }
            )
        }
    }

    if (showTranscribeConfirmation) {
        AlertDialog(
            onDismissRequest = { showTranscribeConfirmation = false },
            title = { Text("Replace current lyrics?") },
            text = { Text("The imported audio will be sent to Gemini for transcription and will replace the lyrics in the current draft.") },
            confirmButton = {
                NothingButton(
                    text = "EXTRACT & REPLACE",
                    onClick = {
                        showTranscribeConfirmation = false
                        viewModel.transcribeImportedAudio(context, replaceExistingLyrics = true)
                    },
                    style = NothingButtonStyle.Primary
                )
            },
            dismissButton = {
                NothingButton(
                    text = "CANCEL",
                    onClick = { showTranscribeConfirmation = false },
                    style = NothingButtonStyle.Secondary
                )
            }
        )
    }
}

private fun formatClock(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
