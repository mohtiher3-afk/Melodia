package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AudioPlayerManager
import com.example.ui.components.NothingDotMatrixWaveform
import com.example.ui.components.pressable
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun ManualSyncDialog(
    lyrics: String,
    audioUrl: String,
    songTitle: String,
    audioPlayerManager: AudioPlayerManager,
    onApplySyncedLyrics: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Parse existing lines and clean out any existing timestamp prefixes for re-syncing
    val parsedLines = remember(lyrics) {
        lyrics.lines()
            .map { line ->
                // Remove [mm:ss.xx] or [mm:ss] if present
                line.replace(Regex("\\[\\d{2}:\\d{2}(\\.\\d{1,3})?\\]"), "").trim()
            }
            .filter { it.isNotBlank() }
    }

    // Map line index -> recorded position in milliseconds
    val recordedTimestamps = remember { mutableStateMapOf<Int, Int>() }
    var nextLineIndex by remember { mutableIntStateOf(0) }

    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by audioPlayerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by audioPlayerManager.duration.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Auto-scroll to active line
    LaunchedEffect(nextLineIndex) {
        if (parsedLines.isNotEmpty() && nextLineIndex in parsedLines.indices) {
            listState.animateScrollToItem((nextLineIndex - 1).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(NothingBlack)
                .testTag("manual_sync_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Text(
                                text = "TAP SYNC // MANUAL TIMESTAMP RECORDER",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = songTitle,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCardSurface)
                            .testTag("close_manual_sync_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Audio Status & Dot-Matrix Waveform
                NothingDotMatrixWaveform(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                )

                // Playback Control & Timestamp Timer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    audioPlayerManager.pauseAudio()
                                } else {
                                    audioPlayerManager.playAudio(audioUrl)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                                .testTag("sync_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NothingWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = formatLrcTimestamp(currentPosition),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Text(
                                text = if (isPlaying) "PLAYING // RECORDING READY" else "AUDIO PAUSED",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 9.sp,
                                color = if (isPlaying) NothingRed else NothingGrey
                            )
                        }
                    }
                }

                // Progress indicator ratio
                Text(
                    text = "${recordedTimestamps.size} / ${parsedLines.size} STAMPED",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (recordedTimestamps.size == parsedLines.size) NothingWhite else NothingGrey
                )

                // PROMINENT TAP BUTTON // Record timestamp for current line
                val currentTargetLine = parsedLines.getOrNull(nextLineIndex) ?: "ALL LINES STAMPED"
                val canTap = isPlaying && nextLineIndex in parsedLines.indices

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (canTap) NothingRed else NothingCardSurface)
                        .border(1.dp, if (canTap) NothingRed else NothingBorder, RoundedCornerShape(20.dp))
                        .pressable(
                            pressedScale = 0.94f,
                            haptic = com.example.ui.components.TapHaptic,
                            enabled = canTap
                        ) {
                            if (nextLineIndex in parsedLines.indices) {
                                recordedTimestamps[nextLineIndex] = currentPosition
                                Toast.makeText(
                                    context,
                                    "Stamped Line ${nextLineIndex + 1}: ${formatLrcTimestamp(currentPosition)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                nextLineIndex = (nextLineIndex + 1).coerceAtMost(parsedLines.size)
                            }
                        }
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .testTag("tap_sync_line_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Tap Sync",
                                tint = NothingWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (!isPlaying) "START AUDIO PLAYBACK TO TAP" else if (nextLineIndex >= parsedLines.size) "ALL LINES RECORDED!" else "TAP TO STAMP LINE ${nextLineIndex + 1}",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                letterSpacing = 1.sp
                            )
                        }

                        if (nextLineIndex in parsedLines.indices) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$currentTargetLine\"",
                                fontSize = 12.sp,
                                color = NothingOffWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Lyric Lines List with Individual Stamps
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LYRIC LINES & TIMESTAMPS",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "RESET ALL",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingGrey,
                            modifier = Modifier.pressable(
                                pressedScale = 0.94f,
                                haptic = com.example.ui.components.TapHaptic,
                                onClick = {
                                    recordedTimestamps.clear()
                                    nextLineIndex = 0
                                    Toast.makeText(context, "Timestamps reset", Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(parsedLines) { idx, lineText ->
                            val timestampMs = recordedTimestamps[idx]
                            val isCurrentTarget = idx == nextLineIndex
                            val isRecorded = timestampMs != null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isCurrentTarget -> NothingDarkGrey
                                            isRecorded -> NothingBlack
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isCurrentTarget -> NothingRed
                                            isRecorded -> NothingBorder
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .pressable(
                                        pressedScale = 0.94f,
                                        haptic = com.example.ui.components.TapHaptic,
                                        onClick = {
                                            nextLineIndex = idx
                                            if (timestampMs != null) {
                                                audioPlayerManager.seekTo(timestampMs)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Index number
                                Text(
                                    text = String.format(Locale.US, "%02d", idx + 1),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (isCurrentTarget) NothingRed else NothingGrey,
                                    fontWeight = FontWeight.Bold
                                )

                                // Timestamp Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isRecorded) NothingRed else NothingDarkGrey)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isRecorded) formatLrcTimestamp(timestampMs!!) else "[--:--.--]",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRecorded) NothingWhite else NothingGrey
                                    )
                                }

                                // Line text
                                Text(
                                    text = lineText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrentTarget) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentTarget) NothingWhite else NothingOffWhite,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Manual Stamp / Overwrite Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NothingBlack)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                        .pressable(
                                            pressedScale = 0.94f,
                                            haptic = com.example.ui.components.TapHaptic,
                                            enabled = isPlaying,
                                            onClick = {
                                                recordedTimestamps[idx] = currentPosition
                                                nextLineIndex = (idx + 1).coerceAtMost(parsedLines.size)
                                                Toast.makeText(context, "Stamped line ${idx + 1}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "STAMP",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = if (isPlaying) NothingRed else NothingGrey,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons: Apply Timings or Copy LRC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingPillButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        testTag = "cancel_sync_button"
                    )

                    NothingPillButton(
                        text = "APPLY SYNCED LRC",
                        onClick = {
                            val formattedLrc = buildString {
                                parsedLines.forEachIndexed { idx, lineText ->
                                    val ms = recordedTimestamps[idx]
                                    if (ms != null) {
                                        append("${formatLrcTimestamp(ms)} $lineText\n")
                                    } else {
                                        append("$lineText\n")
                                    }
                                }
                            }.trim()

                            onApplySyncedLyrics(formattedLrc)
                            Toast.makeText(context, "Synced LRC timings applied!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        isRedAccent = true,
                        modifier = Modifier.weight(1.5f),
                        enabled = recordedTimestamps.isNotEmpty(),
                        testTag = "apply_synced_lrc_button"
                    )
                }
            }
        }
    }
}

/**
 * Formats milliseconds into LRC timestamp format: [mm:ss.xx]
 */
private fun formatLrcTimestamp(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (ms % 1000) / 10
    return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, hundredths)
}