package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.AudioPlayerManager
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import java.util.Locale

@Composable
fun FloatingPlayerBar(
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
                .testTag("floating_player_bar")
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                    .pressable(pressedScale = 0.97f, haptic = TapHaptic) {
                        showExpandedPlayer = true
                    },
                color = NothingDarkSurface
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NothingCoverArt(
                            title = title,
                            genre = artist,
                            imagePath = coverPath,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isPlaying) NothingRed else NothingGrey,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = title,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = artist,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Playback Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { audioPlayerManager.restartTrack() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NothingBlack)
                                .testTag("floating_restart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Restart Track",
                                tint = NothingWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { audioPlayerManager.pauseAudio() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                                .testTag("floating_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NothingWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { audioPlayerManager.stopAudio() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NothingBlack)
                                .testTag("floating_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = NothingGrey,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExpandedPlayer) {
        ExpandedPlayerSheet(
            title = title,
            artist = artist,
            lyrics = lyrics,
            coverPath = coverPath,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onTogglePlay = { audioPlayerManager.pauseAudio() },
            onSeekTo = { ms -> audioPlayerManager.seekTo(ms) },
            onDismiss = { showExpandedPlayer = false }
        )
    }
}

@Composable
fun ExpandedPlayerSheet(
    title: String,
    artist: String,
    lyrics: String,
    coverPath: String?,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    onTogglePlay: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    val effectivePosition = if (isDraggingSlider) sliderValue.toInt() else currentPosition
    val progress = if (duration > 0) (effectivePosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val lines = remember(lyrics) {
        lyrics.lines().filter { it.isNotBlank() }
    }

    // Determine active lyric index based on current playback progress
    val activeLineIndex = remember(progress, lines.size) {
        if (lines.isEmpty()) 0
        else ((progress * lines.size).toInt()).coerceIn(0, lines.size - 1)
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(activeLineIndex) {
        if (lines.isNotEmpty()) {
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
                .background(NothingBlack)
                .testTag("expanded_player_sheet"),
            color = NothingBlack
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
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCardSurface)
                            .testTag("close_expanded_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = NothingWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.player_now_playing),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = stringResource(R.string.player_app_badge),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey
                        )
                    }

                    Spacer(modifier = Modifier.size(36.dp))
                }

                // Syncing Audio Waveform Visualizer with Playback State & Scrubbing
                SyncingAudioWaveformView(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = { ms -> onSeekTo(ms) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                // Track Cover Art & Metadata
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NothingCoverArt(
                        title = title,
                        genre = artist,
                        imagePath = coverPath,
                        modifier = Modifier.size(76.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 12.sp,
                            color = NothingGrey
                        )
                    }
                }

                // Seek Bar & Duration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingCardSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Slider(
                        value = if (isDraggingSlider) sliderValue else currentPosition.toFloat(),
                        onValueChange = {
                            isDraggingSlider = true
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onSeekTo(sliderValue.toInt())
                        },
                        valueRange = 0f..duration.coerceAtLeast(1000).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NothingRed,
                            activeTrackColor = NothingRed,
                            inactiveTrackColor = NothingDarkGrey
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_bar")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(effectivePosition),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatMs(duration),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey
                        )
                    }
                }

                // Main Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeekTo((currentPosition - 10000).coerceAtLeast(0)) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NothingCardSurface)
                            .testTag("rewind_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = NothingWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                            .testTag("main_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NothingWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSeekTo((currentPosition + 10000).coerceAtMost(duration)) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(NothingCardSurface)
                            .testTag("forward_10s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = NothingWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Live Synchronized Lyrics Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.player_live_lyrics),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingRed.copy(alpha = 0.2f))
                                .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.player_realtime_sync),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 8.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (lines.isEmpty()) {
                        Text(
                            text = lyrics.ifBlank { "No lyrics recorded for this track." },
                            fontSize = 13.sp,
                            color = NothingGrey
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lines) { idx, line ->
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
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = line,
                                        fontSize = 14.sp,
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
                                                text = stringResource(R.string.player_state_active),
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
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
