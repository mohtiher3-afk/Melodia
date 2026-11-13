package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.AudioPlayerManager
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.components.CommitHaptic
import com.example.ui.components.TapHaptic
import com.example.ui.components.pressable
import com.example.ui.theme.MelodiaColors
import com.example.ui.theme.SpaceMonoFontFamily

/**
 * Nothing OS style audio player - minimal, monochrome with red accents
 * Based on FloatingPlayerBar but with pure Nothing aesthetics
 */
@Composable
fun MelodiaPlayer(
    audioPlayerManager: AudioPlayerManager,
    modifier: Modifier = Modifier
) {
    val currentUrl by audioPlayerManager.currentUrl.collectAsStateWithLifecycle()
    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val title by audioPlayerManager.currentTitle.collectAsStateWithLifecycle()
    val artist by audioPlayerManager.currentArtist.collectAsStateWithLifecycle()
    val lyrics by audioPlayerManager.currentLyrics.collectAsStateWithLifecycle()
    val coverPath by audioPlayerManager.currentCoverPath.collectAsStateWithLifecycle()
    val currentPosition by audioPlayerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by audioPlayerManager.duration.collectAsStateWithLifecycle()

    var showExpandedPlayer by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = currentUrl != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("melodia_player_bar")
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, MelodiaColors.Border, RoundedCornerShape(24.dp))
                    .pressable(pressedScale = 0.97f, haptic = TapHaptic) {
                        showExpandedPlayer = true
                    },
                color = MelodiaColors.Card
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cover Art & Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingCoverArt(
                            title = title,
                            genre = "",
                            imagePath = coverPath,
                            modifier = Modifier.size(42.dp),
                            testTag = "melodia_player_cover_art"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MelodiaColors.TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = MelodiaColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Controls - Restart, Play/Pause and Close
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NothingButton(
                            text = "",
                            onClick = { audioPlayerManager.restartTrack() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("melodia_restart_button"),
                            style = NothingButtonStyle.IconOnly,
                            icon = Icons.Default.Replay,
                            iconTint = MelodiaColors.TextMain
                        )

                        NothingButton(
                            text = "",
                            onClick = { audioPlayerManager.pauseAudio() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("melodia_play_pause_button"),
                            style = NothingButtonStyle.Primary,
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            iconTint = MelodiaColors.TextMain
                        )

                        NothingButton(
                            text = "",
                            onClick = { audioPlayerManager.stopAudio() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("melodia_close_button"),
                            style = NothingButtonStyle.IconOnly,
                            icon = Icons.Default.Close,
                            iconTint = MelodiaColors.TextSecondary
                        )
                    }
                }
            }
        }

        if (showExpandedPlayer) {
            MelodiaExpandedPlayer(
                title = title,
                artist = artist,
                lyrics = lyrics,
                coverPath = coverPath,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onTogglePlay = { audioPlayerManager.pauseAudio() },
                onSeekTo = { pos -> audioPlayerManager.seekTo(pos) },
                onDismiss = { showExpandedPlayer = false }
            )
        }
    }
}

@Composable
fun MelodiaExpandedPlayer(
    title: String,
    artist: String,
    lyrics: String,
    coverPath: String? = null,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onTogglePlay: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val lazyListState = remember { LazyListState() }
    val lines = remember(lyrics) {
        if (lyrics.isBlank()) emptyList()
        else lyrics.split("\n").filter { it.isNotBlank() }
    }
    val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val activeLineIndex = if (lines.isEmpty()) 0
    else ((progress * lines.size).toInt()).coerceIn(0, lines.size - 1)

    LaunchedEffect(activeLineIndex, lazyListState) {
        if (lines.isNotEmpty() && activeLineIndex > 2) {
            lazyListState.animateScrollToItem((activeLineIndex - 2).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MelodiaColors.Background)
                .testTag("melodia_expanded_player"),
            color = MelodiaColors.Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NothingButton(
                        text = "",
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_melodia_expanded_player_button"),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Close,
                        iconTint = MelodiaColors.TextMain
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.player_now_playing),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = MelodiaColors.Accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = stringResource(R.string.player_app_badge),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 8.sp,
                            color = MelodiaColors.TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Album Art
                NothingCoverArt(
                    title = title,
                    genre = "",
                    imagePath = coverPath,
                    modifier = Modifier
                        .height(240.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    testTag = "melodia_expanded_player_cover_art"
                )

                // Title & Artist
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MelodiaColors.TextMain,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = MelodiaColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Lyrics
                if (lines.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MelodiaColors.Card)
                            .border(1.dp, MelodiaColors.Border, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(lines) { index, line ->
                                val isActive = index == activeLineIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = line,
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 14.sp,
                                        color = if (isActive) MelodiaColors.TextMain else MelodiaColors.TextSecondary,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MelodiaColors.Accent.copy(alpha = 0.2f))
                                                .border(1.dp, MelodiaColors.Accent, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontFamily = SpaceMonoFontFamily,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MelodiaColors.Accent,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = progress,
                        onValueChange = { onSeekTo((it * duration).toInt()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MelodiaColors.Accent,
                            activeTrackColor = MelodiaColors.Accent,
                            inactiveTrackColor = MelodiaColors.TextSecondary.copy(alpha = 0.3f)
                        ),
                        valueRange = 0f..duration.coerceAtLeast(1000).toFloat()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(currentPosition),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = MelodiaColors.TextSecondary
                        )
                        Text(
                            text = formatMs(duration),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = MelodiaColors.TextSecondary
                        )
                    }
                }

                // Controls - Rewind, Play/Pause, Forward
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NothingButton(
                            text = "",
                            onClick = { onSeekTo((currentPosition - 10000).coerceAtLeast(0)) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("melodia_rewind_10s_button"),
                            style = NothingButtonStyle.IconOnly,
                            icon = Icons.Default.Replay10,
                            iconTint = MelodiaColors.TextMain
                        )

                        NothingButton(
                            text = "",
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("melodia_main_play_pause_button"),
                            style = NothingButtonStyle.Primary,
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            iconTint = MelodiaColors.TextMain
                        )

                        NothingButton(
                            text = "",
                            onClick = { onSeekTo((currentPosition + 10000).coerceAtMost(duration)) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("melodia_forward_10s_button"),
                            style = NothingButtonStyle.IconOnly,
                            icon = Icons.Default.Forward10,
                            iconTint = MelodiaColors.TextMain
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Dot-matrix icons for Nothing OS aesthetic
 */
enum class DotIcon { PLAY, PAUSE, CLOSE }

@Composable
fun DotMatrixIcon(
    icon: DotIcon,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = MelodiaColors.TextMain,
    animated: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = CircleShape
    val containerColor = MelodiaColors.Background

    Box(
        modifier = modifier
            .size(size)
            .background(containerColor)
            .clip(shape)
            .testTag("dot_matrix_icon_${icon.name}")
    ) {
        when (icon) {
            DotIcon.PLAY -> {
                DotMatrixPlayIcon(size = size, tint = tint, animated = animated)
            }
            DotIcon.PAUSE -> {
                DotMatrixPauseIcon(size = size, tint = tint)
            }
            DotIcon.CLOSE -> {
                DotMatrixCloseIcon(size = size, tint = tint)
            }
        }
    }
}

@Composable
fun DotMatrixPlayIcon(
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = MelodiaColors.TextMain,
    animated: Boolean = false
) {
    // Play: right-pointing triangle using Canvas DrawScope
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val w = this.size.width
        val h = this.size.height
        val inset = w * 0.25f
        drawPath(
            color = tint,
            path = Path().apply {
                moveTo(inset, inset)
                lineTo(w - inset, h / 2f)
                lineTo(inset, h - inset)
                close()
            }
        )
    }
}

@Composable
fun DotMatrixPauseIcon(
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = MelodiaColors.TextMain
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(4.dp, size * 0.6f).background(tint, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.size(4.dp, size * 0.6f).background(tint, RoundedCornerShape(2.dp)))
    }
}

@Composable
fun DotMatrixCloseIcon(
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = MelodiaColors.TextMain
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val w = this.size.width
        val h = this.size.height
        val inset = w * 0.25f
        drawLine(
            color = tint,
            start = Offset(inset, inset),
            end = Offset(w - inset, h - inset),
            strokeWidth = 2f
        )
        drawLine(
            color = tint,
            start = Offset(w - inset, inset),
            end = Offset(inset, h - inset),
            strokeWidth = 2f
        )
    }
}

/**
 * Cover art with Nothing OS styling
 */
@Composable
fun NothingCoverArt(
    title: String,
    genre: String,
    imagePath: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    // Placeholder implementation - in real app would load from imagePath
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MelodiaColors.Elevated)
            .border(1.dp, MelodiaColors.Border, RoundedCornerShape(20.dp))
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(1).uppercase(),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MelodiaColors.Accent
        )
    }
}