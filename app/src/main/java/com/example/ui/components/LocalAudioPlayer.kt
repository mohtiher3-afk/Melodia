package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AudioPlayerManager
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun LocalAudioPlayer(
    audioPlayerManager: AudioPlayerManager,
    audioUrl: String,
    title: String,
    streamSource: String = "LOCAL AUDIO",
    modifier: Modifier = Modifier,
    onSendToMainPlayer: () -> Unit = {},
    onSaveToLibrary: () -> Unit = {}
) {
    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by audioPlayerManager.currentPosition.collectAsStateWithLifecycle()
    val durationMs by audioPlayerManager.duration.collectAsStateWithLifecycle()

    val currentSec = currentPositionMs / 1000
    val durationSec = if (durationMs > 0) durationMs / 1000 else 15

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NothingBlack)
            .border(1.dp, NothingRed, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("raw_audio_stream_player")
    ) {
        // Stream Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) NothingRed else NothingWhite)
                )
                Text(
                    text = "AUDIO PREVIEW // LOCAL",
                    fontFamily = NothingDotMatrixFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NothingCardSurface)
                    .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "LOCAL FILE",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title and Source
        Text(
            text = title.ifBlank { "Audio Preview" },
            fontFamily = SpaceMonoFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NothingWhite
        )
        Text(
            text = "SOURCE: $streamSource",
            fontFamily = SpaceMonoFontFamily,
            fontSize = 10.sp,
            color = NothingRed
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Audio Meter Visualizer
        DotMatrixAudioMeter(
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Slider & Timers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                color = NothingWhite
            )
            Text(
                text = String.format("%02d:%02d", durationSec / 60, durationSec % 60),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                color = NothingGrey
            )
        }

        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        Slider(
            value = progress,
            onValueChange = { targetFraction ->
                if (durationMs > 0) {
                    audioPlayerManager.seekTo((targetFraction * durationMs).toInt())
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = NothingRed,
                activeTrackColor = NothingRed,
                inactiveTrackColor = NothingCardSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stream_seek_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingRed)
                    .clickable {
                        if (isPlaying) {
                            audioPlayerManager.pauseAudio()
                        } else {
                            audioPlayerManager.playAudio(audioUrl)
                        }
                    }
                    .padding(vertical = 10.dp)
                    .testTag("stream_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Stream",
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPlaying) "PAUSE" else "PLAY",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingCardSurface)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        audioPlayerManager.seekTo(0)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("stream_restart_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Restart Stream",
                    tint = NothingWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NothingCardSurface)
                .border(1.dp, NothingRed, RoundedCornerShape(10.dp))
                .clickable { onSaveToLibrary() }
                .padding(vertical = 8.dp)
                .testTag("stream_download_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SAVE AUDIO TO LOCAL LIBRARY",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingRed
            )
        }
    }
}
