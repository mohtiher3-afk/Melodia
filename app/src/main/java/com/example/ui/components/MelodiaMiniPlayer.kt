package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.example.R
import com.example.domain.AudioPlayerManager
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

/**
 * Mini player bar shown above bottom navigation when a track is loaded.
 * Tapping opens the full PlayerScreen. Driven from AudioPlayerManager StateFlow.
 */
@Composable
fun MelodiaMiniPlayer(
    audioPlayerManager: AudioPlayerManager,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by audioPlayerManager.currentUrl.collectAsStateWithLifecycle()
    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val title by audioPlayerManager.currentTitle.collectAsStateWithLifecycle()
    val artist by audioPlayerManager.currentArtist.collectAsStateWithLifecycle()
    val coverPath by audioPlayerManager.currentCoverPath.collectAsStateWithLifecycle()

    // Equalizer bars animation state
    val equalizerBars = remember {
        (0..3).map { mutableStateOf(0.3f) }
    }

    // Animate equalizer when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
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

    AnimatedVisibility(
        visible = currentUrl != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                .pressable(pressedScale = 0.98f) {
                    onOpenFullPlayer()
                },
            color = NothingCardSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cover Art & Track Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini cover art
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.take(1).uppercase(),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artist,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 9.sp,
                            color = NothingGrey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls - Previous, Play/Pause, Next, Equalizer indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NothingButton(
                        text = "",
                        onClick = {
                            val currentPosition = audioPlayerManager.currentPosition.value
                            audioPlayerManager.seekTo((currentPosition - 10000).coerceAtLeast(0))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_rewind_button"),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Replay10,
                        iconTint = NothingWhite
                    )

                    NothingButton(
                        text = "",
                        onClick = { audioPlayerManager.pauseAudio() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_play_pause_button"),
                        style = NothingButtonStyle.Primary,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        iconTint = NothingWhite
                    )

                    NothingButton(
                        text = "",
                        onClick = {
                            val currentPosition = audioPlayerManager.currentPosition.value
                            val duration = audioPlayerManager.duration.value
                            audioPlayerManager.seekTo((currentPosition + 10000).coerceAtMost(duration))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("mini_forward_button"),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Forward10,
                        iconTint = NothingWhite
                    )

                    // Equalizer bars indicator (shows when playing)
                    if (isPlaying) {
                        Row(
                            modifier = Modifier
                                .width(24.dp)
                                .height(20.dp)
                                .padding(start = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            equalizerBars.forEachIndexed { idx, bar ->
                                val animatedHeight = animateFloatAsState(
                                    targetValue = bar.value * 16f + 4f, // scale to 4-20dp
                                    animationSpec = tween(80),
                                    label = "mini_eq_bar"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(animatedHeight.value.dp)
                                        .background(NothingRed)
                                        .clip(RoundedCornerShape(2.dp))
                                        .align(Alignment.Bottom)
                                )
                            }
                        }
                    } else {
                        // Static equalizer icon when paused
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = NothingGrey,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}