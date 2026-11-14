package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AudioPlayerManager
import com.example.ui.nothing.DotIcon
import com.example.ui.nothing.Nt
import com.example.ui.nothing.NtBody
import com.example.ui.nothing.NtCard
import com.example.ui.nothing.NtIconButton
import com.example.ui.nothing.NtProgressBar

/**
 * Mini player shown above the bottom navigation when a track is loaded.
 * Built from the Nothing-style UI kit (see docs/NOTHING_UI_SPEC.md). Tapping the card
 * opens the full player via [onExpand]. Driven entirely by [AudioPlayerManager] state -
 * the same source of truth as [FloatingPlayerBar].
 */
@Composable
fun MelodiaMiniPlayer(
    audioPlayerManager: AudioPlayerManager,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUrl by audioPlayerManager.currentUrl.collectAsStateWithLifecycle()
    val isPlaying by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val title by audioPlayerManager.currentTitle.collectAsStateWithLifecycle()
    val artist by audioPlayerManager.currentArtist.collectAsStateWithLifecycle()
    val currentPosition by audioPlayerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by audioPlayerManager.duration.collectAsStateWithLifecycle()

    // Only show when a track is loaded.
    if (currentUrl == null) return

    val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    NtCard(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Nt.ScreenPadding, vertical = 4.dp)
            .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NtBody(text = title)
                NtProgressBar(progress = progress, label = artist)
            }

            // Play/pause is the only control; tap the card itself to expand.
            NtIconButton(
                icon = if (isPlaying) DotIcon.PAUSE else DotIcon.PLAY,
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = { audioPlayerManager.pauseAudio() },
            )
        }
    }
}
