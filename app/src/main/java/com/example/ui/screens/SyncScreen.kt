package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LyricsExportManager
import com.example.ui.components.GlyphCard
import com.example.ui.components.KaraokeLyricsView
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun SyncScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val player = viewModel.audioPlayerManager
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val playbackPosition by player.currentPosition.collectAsStateWithLifecycle()
    val playbackDuration by player.duration.collectAsStateWithLifecycle()
    val playbackUrl by player.currentUrl.collectAsStateWithLifecycle()

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

        GlyphCard(headerTitle = "LOCAL WHISPER AUDIO ALIGNMENT") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header with StatusBadge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALIGNMENT STATUS",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )
                    StatusBadge(
                        status = when {
                            state.isAligningAudio -> "PROCESSING"
                            state.karaokeLyrics != null -> "READY"
                            else -> "IDLE"
                        }
                    )
                }

                if (state.isLocalAudioActive && state.localAudioMetadata != null) {
                    Text(
                        text = "IMPORTED AUDIO: ${state.localAudioMetadata!!.title}",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingWhite,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "NO LOCAL AUDIO TRACK IMPORTED",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )
                    NothingPillButton(
                        text = "IMPORT LOCAL AUDIO FILE",
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NothingPillButton(
                    text = if (state.isAligningAudio) "ALIGNING WITH WHISPER..." else "RUN WHISPER ALIGNMENT",
                    onClick = { viewModel.syncLyricsWithAudio(context) },
                    isPrimary = true
                )

                if (karaoke != null) {
                    Text(
                        text = "WORD TIMESTAMPS GENERATED (${karaoke.wordTimings.size} WORDS)",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NothingPillButton(
                            text = "SHARE LRC",
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
                            modifier = Modifier.weight(1f)
                        )

                        NothingPillButton(
                            text = "SHARE JSON",
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
                        NothingPillButton(
                            text = when {
                                playbackUrl == null -> "IMPORT AUDIO TO PREVIEW"
                                isPlaying -> "PAUSE PREVIEW"
                                else -> "PLAY PREVIEW"
                            },
                            onClick = {
                                playbackUrl?.let { player.playAudio(it) }
                            },
                            enabled = playbackUrl != null,
                            isPrimary = playbackUrl != null,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${formatClock(playbackPosition)} / ${formatClock(playbackDuration)}",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatClock(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

