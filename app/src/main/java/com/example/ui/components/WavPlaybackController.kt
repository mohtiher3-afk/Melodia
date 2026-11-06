package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AudioPlayerManager
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import java.util.Locale

@Composable
fun WavPlaybackController(
    audioPlayerManager: AudioPlayerManager,
    audioUrl: String,
    title: String,
    artist: String,
    lyrics: String = "",
    coverPath: String? = null,
    modifier: Modifier = Modifier
) {
    val currentUrl by audioPlayerManager.currentUrl.collectAsStateWithLifecycle()
    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by audioPlayerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by audioPlayerManager.duration.collectAsStateWithLifecycle()

    val isThisTrackActive = currentUrl == audioUrl
    val activeIsPlaying = isThisTrackActive && isPlaying
    val activePosition = if (isThisTrackActive) currentPosition else 0
    val activeDuration = if (isThisTrackActive) duration else 0

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    val displayPosition = if (isDraggingSlider) sliderValue.toInt() else activePosition
    val safeDuration = activeDuration.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NothingCardSurface)
            .border(1.dp, NothingRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("wav_playback_controller"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (activeIsPlaying) NothingRed else NothingGrey,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "SYNTHESIZED WAV PREVIEW",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "${formatMs(displayPosition)} / ${formatMs(activeDuration)}",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )
        }

        // Scrubbing Bar / Slider
        Slider(
            value = displayPosition.toFloat().coerceIn(0f, safeDuration.toFloat()),
            onValueChange = {
                isDraggingSlider = true
                sliderValue = it
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                if (!isThisTrackActive) {
                    audioPlayerManager.playTrack(
                        url = audioUrl,
                        title = title,
                        artist = artist,
                        lyrics = lyrics,
                        coverPath = coverPath
                    )
                }
                audioPlayerManager.seekTo(sliderValue.toInt())
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingDarkGrey
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wav_scrub_slider")
        )

        // Control Buttons Row: Restart, Rewind 10s, Play/Pause, Forward 10s
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restart Button
            IconButton(
                onClick = {
                    if (isThisTrackActive) {
                        audioPlayerManager.restartTrack()
                    } else {
                        audioPlayerManager.playTrack(
                            url = audioUrl,
                            title = title,
                            artist = artist,
                            lyrics = lyrics,
                            coverPath = coverPath
                        )
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NothingDarkGrey)
                    .testTag("wav_restart_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Restart Track",
                    tint = NothingWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Rewind 10s Button
            IconButton(
                onClick = {
                    if (isThisTrackActive) {
                        audioPlayerManager.seekTo((activePosition - 10000).coerceAtLeast(0))
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NothingDarkGrey)
                    .testTag("wav_rewind_10s_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = NothingWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play / Pause Button
            IconButton(
                onClick = {
                    if (isThisTrackActive) {
                        audioPlayerManager.pauseAudio()
                    } else {
                        audioPlayerManager.playTrack(
                            url = audioUrl,
                            title = title,
                            artist = artist,
                            lyrics = lyrics,
                            coverPath = coverPath
                        )
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
                    .testTag("wav_play_pause_button")
            ) {
                Icon(
                    imageVector = if (activeIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (activeIsPlaying) "Pause Preview" else "Play Preview",
                    tint = NothingWhite,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Forward 10s Button
            IconButton(
                onClick = {
                    if (isThisTrackActive) {
                        audioPlayerManager.seekTo((activePosition + 10000).coerceAtMost(activeDuration))
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NothingDarkGrey)
                    .testTag("wav_forward_10s_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = NothingWhite,
                    modifier = Modifier.size(20.dp)
                )
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
