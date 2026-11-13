package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Equalizer
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.AudioPlayerManager
import com.example.domain.RepeatMode
import com.example.network.KaraokeLyrics
import com.example.network.WordTiming
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.components.CommitHaptic
import com.example.ui.components.TapHaptic
import com.example.ui.components.pressable
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    audioPlayerManager: AudioPlayerManager,
    onDismiss: () -> Unit,
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
    val shuffleMode by audioPlayerManager.shuffleMode.collectAsStateWithLifecycle()
    val repeatMode by audioPlayerManager.repeatMode.collectAsStateWithLifecycle()
    val volume by audioPlayerManager.volume.collectAsStateWithLifecycle()

    // Lyrics from karaoke data (if available)
    val karaokeLyrics = remember { mutableStateOf<KaraokeLyrics?>(null) }
    val wordTimings by remember { mutableStateOf<List<WordTiming>>(emptyList()) }

    val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    // Compute lyricPrev/lyricNow from wordTimings
    val (lyricPrev, lyricNow) = remember(currentPosition, wordTimings) {
        computeCurrentLyrics(currentPosition, wordTimings)
    }

    val lazyListState = remember { LazyListState() }
    val lines = remember(lyrics) {
        if (lyrics.isBlank()) emptyList()
        else lyrics.split("\n").filter { it.isNotBlank() }
    }
    val activeLineIndex = if (lines.isEmpty()) 0
    else ((progress * lines.size).toInt()).coerceIn(0, lines.size - 1)

    // Waveform equalizer bars animation
    val equalizerBars = remember {
        (0..5).map { mutableStateOf(0.3f + (Math.random() * 0.7).toFloat()) }
    }

    // Animate equalizer when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                equalizerBars.forEach { bar ->
                    bar.value = 0.2f + (Math.random() * 0.8).toFloat()
                }
                delay(120)
            }
        } else {
            equalizerBars.forEach { bar ->
                bar.value = 0.1f
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(NothingBlack)) {
        // Background cover art with blur/dim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Cover art
            NothingCoverArt(
                title = title,
                genre = "",
                imagePath = coverPath,
                modifier = Modifier
                    .height(280.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                testTag = "player_cover_art"
            )
        }

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to NothingBlack.copy(alpha = 0f),
                        0.6f to NothingBlack.copy(alpha = 0.8f),
                        1f to NothingBlack
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar - close + NOW PLAYING badge
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
                        .testTag("player_close_button"),
                    style = NothingButtonStyle.IconOnly,
                    icon = Icons.Default.Close,
                    iconTint = NothingWhite
                )

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

                // Equalizer indicator when playing
                if (isPlaying) {
                    Row(
                        modifier = Modifier.size(48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (0..3).forEach { idx ->
                            val barHeight = remember { mutableStateOf(4f) }
                            LaunchedEffect(isPlaying, idx) {
                                if (isPlaying) {
                                    while (true) {
                                        delay((idx * 80 + 100).toLong())
                                        barHeight.value = 4f + (Math.random() * 16).toFloat()
                                    }
                                } else {
                                    barHeight.value = 4f
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight.value.dp)
                                    .background(NothingRed)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Cover Art
            NothingCoverArt(
                title = title,
                genre = "",
                imagePath = coverPath,
                modifier = Modifier
                    .height(240.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                testTag = "player_expanded_cover_art"
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

            // Lyrics area with karaoke highlighting
            if (lines.isNotEmpty() || wordTimings.isNotEmpty()) {
                LyricsKaraokeView(
                    lines = if (wordTimings.isNotEmpty()) emptyList() else lines,
                    wordTimings = wordTimings,
                    currentPosition = currentPosition,
                    duration = duration,
                    activeLineIndex = activeLineIndex,
                    lyricPrev = lyricPrev,
                    lyricNow = lyricNow,
                    lazyListState = lazyListState
                )
            } else {
                // Empty lyrics placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.player_no_lyrics),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 14.sp,
                        color = NothingGrey
                    )
                }
            }

            // Waveform seek bar
            WaveformSeekBar(
                progress = progress,
                duration = duration,
                currentPosition = currentPosition,
                isPlaying = isPlaying,
                equalizerBars = equalizerBars,
                onSeek = { pos -> audioPlayerManager.seekTo(pos) }
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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

            // Volume slider
            VolumeSlider(
                volume = volume,
                onVolumeChange = { vol -> audioPlayerManager.setVolume(vol) }
            )

            // Controls - Shuffle, Rewind, Play/Pause, Forward, Repeat
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NothingButton(
                    text = "",
                    onClick = { audioPlayerManager.toggleShuffle() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_shuffle_button"),
                    style = if (shuffleMode) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
                    icon = Icons.Default.Shuffle,
                    iconTint = if (shuffleMode) NothingRed else NothingWhite
                )

                NothingButton(
                    text = "",
                    onClick = { audioPlayerManager.seekTo((currentPosition - 10000).coerceAtLeast(0)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_rewind_10s_button"),
                    style = NothingButtonStyle.IconOnly,
                    icon = Icons.Default.Replay10,
                    iconTint = NothingWhite
                )

                NothingButton(
                    text = "",
                    onClick = { audioPlayerManager.pauseAudio() },
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_main_play_pause_button"),
                    style = NothingButtonStyle.Primary,
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    iconTint = NothingWhite
                )

                NothingButton(
                    text = "",
                    onClick = { audioPlayerManager.seekTo((currentPosition + 10000).coerceAtMost(duration)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_forward_10s_button"),
                    style = NothingButtonStyle.IconOnly,
                    icon = Icons.Default.Forward10,
                    iconTint = NothingWhite
                )

                NothingButton(
                    text = "",
                    onClick = { audioPlayerManager.toggleRepeat() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_repeat_button"),
                    style = when (repeatMode) {
                        RepeatMode.OFF -> NothingButtonStyle.Secondary
                        com.example.domain.RepeatMode.ALL -> NothingButtonStyle.Primary
                        com.example.domain.RepeatMode.ONE -> NothingButtonStyle.Primary
                        else -> NothingButtonStyle.Secondary
                    },
                    icon = Icons.Default.Repeat,
                    iconTint = when (repeatMode) {
                        RepeatMode.OFF -> NothingWhite
                        com.example.domain.RepeatMode.ALL -> NothingRed
                        com.example.domain.RepeatMode.ONE -> NothingRed
                        else -> NothingWhite
                    }
                )
            }
        }
    }
}

@Composable
fun LyricsKaraokeView(
    lines: List<String>,
    wordTimings: List<WordTiming>,
    currentPosition: Int,
    duration: Int,
    activeLineIndex: Int,
    lyricPrev: String,
    lyricNow: String,
    lazyListState: LazyListState
) {
    // If we have word-level timings, show karaoke view
    if (wordTimings.isNotEmpty()) {
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous line
                if (lyricPrev.isNotBlank()) {
                    Text(
                        text = lyricPrev,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 13.sp,
                        color = NothingGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Current line - highlighted
                Text(
                    text = lyricNow,
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Next word preview (optional)
            }
        }
    } else {
        // Fallback to line-based lyrics
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
}

@Composable
fun WaveformSeekBar(
    progress: Float,
    duration: Int,
    currentPosition: Int,
    isPlaying: Boolean,
    equalizerBars: List<androidx.compose.runtime.MutableState<Float>>,
    onSeek: (Int) -> Unit
) {
    val barWidth = 4.dp
    val barSpacing = 2.dp
    val barCount = 60 // Number of bars in waveform

    val modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                    val totalWidth = this.size.width
                    val seekProgress = (offset.x / totalWidth).coerceIn(0f, 1f)
                    onSeek((seekProgress * duration).toInt())
                },
                onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, offset: androidx.compose.ui.geometry.Offset ->
                    val totalWidth = this.size.width
                    val currentX = change.position.x
                    val dragProgress = (currentX / totalWidth).coerceIn(0f, 1f)
                    onSeek((dragProgress * duration).toInt())
                }
            )
        }

    Box(modifier = modifier) {
        if (isPlaying) {
            // Show animated equalizer bars
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(barSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                equalizerBars.forEach { bar ->
                    val animatedHeight = animateFloatAsState(
                        targetValue = bar.value,
                        animationSpec = tween(80),
                        label = "eq_bar"
                    )
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(animatedHeight.value.dp)
                            .background(NothingRed)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        } else {
            // Show static waveform (progress-based)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(barSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                (0 until barCount).forEach { i ->
                    val barProgress = (i + 1) / barCount.toFloat()
                    val isActive = barProgress <= progress
                    val heightFraction = if (isPlaying) {
                        // Random for playing
                        0.3f + (Math.random() * 0.7).toFloat()
                    } else {
                        // Static waveform shape
                        0.2f + kotlin.math.abs(Math.sin(i * 0.5) * 0.3f).toFloat()
                    }
                    val barHeightPx = (heightFraction * with(LocalDensity.current) { 40.dp.toPx() }).coerceIn(with(LocalDensity.current) { 4.dp.toPx() }, with(LocalDensity.current) { 40.dp.toPx() })
                    val barHeightDp = with(LocalDensity.current) { barHeightPx.toDp() }

                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barHeightDp)
                            .background(if (isActive) NothingRed else NothingWhite.copy(alpha = 0.3f))
                            .clip(RoundedCornerShape(2.dp))
                            .align(Alignment.Bottom)
                    )
                }
            }
        }
    }
}

@Composable
fun VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val volumeIcon = if (volume > 0.01f) Icons.Default.VolumeUp else Icons.Default.VolumeOff

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = volumeIcon,
            contentDescription = "Volume",
            tint = NothingWhite,
            modifier = Modifier.size(20.dp)
        )
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingWhite.copy(alpha = 0.2f)
            ),
            valueRange = 0f..1f
        )
    }
}

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
            .background(NothingCardSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(20.dp))
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(1).uppercase(),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NothingRed
        )
    }
}

private fun computeCurrentLyrics(
    currentPosition: Int,
    wordTimings: List<WordTiming>
): Pair<String, String> {
    var prev = ""
    var now = ""

    for (i in wordTimings.indices) {
        val word = wordTimings[i]
        if (word.startTimeMs <= currentPosition && word.endTimeMs >= currentPosition) {
            now = word.text
            // Find previous word
            if (i > 0) {
                prev = wordTimings[i - 1].text
            }
            break
        } else if (word.endTimeMs < currentPosition) {
            prev = word.text
        }
    }

    return prev to now
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}