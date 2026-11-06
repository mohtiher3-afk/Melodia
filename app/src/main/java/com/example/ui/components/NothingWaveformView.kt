package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun NothingDotMatrixWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    // Pre-seed pseudo heights based on index for deterministic aesthetic
    val baseHeights = remember {
        List(barCount) { i ->
            val factor = kotlin.math.sin(i * 0.4f) * 0.5f + 0.5f
            0.2f + 0.8f * factor
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .padding(8.dp)
            .testTag("nothing_dot_matrix_waveform")
    ) {
        val width = size.width
        val height = size.height
        val barWidth = width / barCount
        val dotRadius = (barWidth * 0.35f).coerceAtMost(3.5f)

        for (i in 0 until barCount) {
            val base = baseHeights[i]
            val dynamicFactor = if (isPlaying) {
                kotlin.math.sin(phase + i * 0.3f) * 0.4f + 0.8f
            } else {
                0.4f
            }
            val activeHeight = (height * base * dynamicFactor).coerceIn(10f, height - 4f)
            val x = i * barWidth + barWidth / 2f

            // Draw dot matrix column (multiple stacked dots)
            val dotSpacing = 6f
            val numDots = (activeHeight / dotSpacing).toInt().coerceAtLeast(2)
            val startY = height - 4f

            for (d in 0 until numDots) {
                val y = startY - (d * dotSpacing)
                val isHighlighted = isPlaying && (i in 10..18)
                val dotColor = if (isHighlighted) NothingRed else if (d < numDots * 0.7f) NothingWhite else NothingGrey

                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun SyncingAudioWaveformView(
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    modifier: Modifier = Modifier,
    barCount: Int = 44,
    onSeek: ((Int) -> Unit)? = null
) {
    val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "sync_waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_waveform_phase"
    )

    val baseHeights = remember(barCount) {
        List(barCount) { i ->
            val factor = kotlin.math.sin(i * 0.35f) * 0.5f + 0.5f
            0.25f + 0.75f * factor
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
            .pointerInput(duration) {
                if (onSeek != null && duration > 0) {
                    detectTapGestures { offset ->
                        val tappedRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetMs = (tappedRatio * duration).toInt()
                        onSeek(targetMs)
                    }
                }
            }
            .padding(12.dp)
            .testTag("syncing_audio_waveform")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / barCount
            val dotRadius = (barWidth * 0.3f).coerceAtMost(3f)
            val progressX = width * progress

            for (i in 0 until barCount) {
                val base = baseHeights[i]
                val x = i * barWidth + barWidth / 2f
                val isElapsed = x <= progressX

                val dynamicFactor = if (isPlaying) {
                    kotlin.math.sin(phase + i * 0.25f) * 0.3f + 0.85f
                } else {
                    0.5f
                }

                val activeHeight = (height * base * dynamicFactor).coerceIn(12f, height - 6f)
                val dotSpacing = 5f
                val numDots = (activeHeight / dotSpacing).toInt().coerceAtLeast(3)
                val startY = height - 4f

                for (d in 0 until numDots) {
                    val y = startY - (d * dotSpacing)
                    val dotColor = when {
                        isElapsed && d >= numDots - 2 -> NothingRed
                        isElapsed -> NothingWhite
                        else -> NothingGrey.copy(alpha = 0.5f)
                    }

                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }

            // Draw Playhead progress line
            if (duration > 0) {
                drawLine(
                    color = NothingRed,
                    start = Offset(progressX, 0f),
                    end = Offset(progressX, height),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

@Composable
fun WaveformLyricsDialog(
    song: Song,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDismiss: () -> Unit,
    currentPosition: Int = 0,
    duration: Int = 180000,
    onSeek: ((Int) -> Unit)? = null
) {
    // Simulate active lyric line index progression when playing
    val lines = remember(song.lyrics) {
        song.lyrics.lines().filter { it.isNotBlank() }
    }
    var activeLineIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying) {
        if (isPlaying && lines.isNotEmpty()) {
            while (true) {
                delay(2500L)
                activeLineIndex = (activeLineIndex + 1) % lines.size
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("waveform_lyrics_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(NothingRed)
                            )
                            Text(
                                text = "WAVEFORM & SYNCHRONIZED TIMINGS",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NothingCardSurface)
                            .testTag("close_waveform_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Syncing Audio Waveform Visualizer with Playback State & Scrubbing
                SyncingAudioWaveformView(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )

                // Play / Pause Bar & Sync Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isPlaying) NothingRed else NothingGrey,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPlaying) "PLAYING // SYNC ACTIVE" else "AUDIO PAUSED",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) NothingRed else NothingWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingBlack)
                            .border(1.dp, if (isPlaying) NothingRed else NothingBorder, RoundedCornerShape(10.dp))
                            .clickable { onTogglePlay() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_waveform_play_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NothingWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isPlaying) "PAUSE" else "PLAY AUDIO",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }

                // Synchronized Lyrics Section with Pulsing Red Line Highlight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "SYNCHRONIZED TIMING MARKERS",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (lines.isEmpty()) {
                        Text(
                            text = song.lyrics,
                            fontSize = 12.sp,
                            color = NothingGrey
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            lines.forEachIndexed { idx, line ->
                                val isCurrent = isPlaying && idx == activeLineIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) NothingDarkGrey else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isCurrent) NothingRed else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = line,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) NothingWhite else NothingGrey,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NothingRed)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontFamily = SpaceMonoFontFamily,
                                                fontSize = 8.sp,
                                                color = NothingWhite,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                NothingPillButton(
                    text = "CLOSE VIEWER",
                    onClick = onDismiss,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "close_waveform_view_button"
                )
            }
        }
    }
}
