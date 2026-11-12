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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.text.style.TextAlign
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
                            testTag = "floating_player_cover_art"
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
                                color = NothingWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                    // Controls - Restart, Play/Pause and Close
                    Row(
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
                            DotMatrixIcon(
                                icon = if (isPlaying) DotIcon.PAUSE else DotIcon.PLAY,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                size = 20.dp,
                                tint = NothingWhite,
                                animated = isPlaying
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
                            DotMatrixIcon(
                                icon = DotIcon.CLOSE,
                                contentDescription = "Close Player",
                                size = 16.dp,
                                tint = NothingGrey
                            )
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
                onSeekTo = { pos -> audioPlayerManager.seekTo(pos) },
                onDismiss = { showExpandedPlayer = false }
            )
        }
    }
}

@Composable
fun ExpandedPlayerSheet(
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
    val lazyListState = rememberLazyListState()
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
                        DotMatrixIcon(
                            icon = DotIcon.CLOSE,
                            contentDescription = "Minimize",
                            size = 22.dp,
                            tint = NothingWhite
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
                            fontSize = 8.sp,
                            color = NothingGrey,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.size(36.dp))
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
                    testTag = "expanded_player_cover_art"
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
                        color = NothingWhite,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey,
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
                            .background(NothingCardSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
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
                                        color = if (isActive) NothingWhite else NothingGrey,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NothingRed.copy(alpha = 0.2f))
                                                .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontFamily = SpaceMonoFontFamily,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingRed,
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
                            thumbColor = NothingRed,
                            activeTrackColor = NothingRed,
                            inactiveTrackColor = NothingGrey.copy(alpha = 0.3f)
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
                            color = NothingGrey
                        )
                        Text(
                            text = formatMs(duration),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey
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
                                .background(NothingBlack)
                                .border(2.dp, NothingRed, CircleShape)
                                .testTag("main_play_pause_button")
                        ) {
                            DotMatrixIcon(
                                icon = if (isPlaying) DotIcon.PAUSE else DotIcon.PLAY,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                size = 36.dp,
                                tint = NothingWhite,
                                animated = isPlaying
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