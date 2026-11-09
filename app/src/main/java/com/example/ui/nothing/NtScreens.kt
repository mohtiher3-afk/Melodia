package com.example.ui.nothing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NothingDotMatrixFontFamily

// Stateless screen bodies. Put them inside NtAppShell and feed them from the ViewModel state.
// Child index passed to staggeredEnter() controls the entrance order.

@Composable
private fun ChipRow(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEachIndexed { i, text -> NtChip(text, selected = i == selected, onClick = { onSelect(i) }) }
    }
}

// ---------------------------------------------------------------- 1. Studio

private val GeneratingLed: @Composable () -> Unit = { NtPulsingLed(color = Nt.White) }

@Composable
fun ColumnScope.NtStudioContent(
    prompt: String,
    genres: List<String>,
    isGenerating: Boolean,
    sectionLabel: String,
    lines: List<String>,
    writingLine: String,
    onGenerate: () -> Unit
) {
    NtTitle(stringResource(R.string.nt_studio_title), Modifier.staggeredEnter(1))
    NtCard(Modifier.staggeredEnter(2)) {
        NtLabel(stringResource(R.string.nt_prompt))
        NtBody(prompt)
        ChipRow(genres, selected = -1, onSelect = {})
    }
    NtButton(
        text = stringResource(if (isGenerating) R.string.nt_generating else R.string.nt_generate),
        onClick = { if (!isGenerating) onGenerate() },
        modifier = Modifier.fillMaxWidth().staggeredEnter(3),
        style = NtButtonStyle.Primary,
        leading = if (isGenerating) GeneratingLed else null
    )
    if (isGenerating) {
        NtProgressBar(progress = null, label = stringResource(R.string.nt_generating_desc))
    }
    NtCard(Modifier.staggeredEnter(4)) {
        NtLabel(sectionLabel, color = Nt.RedText)
        lines.forEach { NtBody(it) }
        if (isGenerating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NtBody(writingLine)
                NtCaret()
            }
            NtSkeleton(Modifier.fillMaxWidth())
            NtSkeleton(Modifier.fillMaxWidth(0.6f))
        }
    }
}

// ---------------------------------------------------------------- 2. Lyrics

@Composable
fun ColumnScope.NtLyricsContent(
    sections: List<String>,
    selectedSection: Int,
    onSelectSection: (Int) -> Unit,
    onAddSection: () -> Unit,
    lines: List<String>,
    rhymeWord: String,
    rhymes: List<String>,
    rhymesLoading: Boolean,
    onPlay: () -> Unit,
    onNext: () -> Unit
) {
    NtTitle(stringResource(R.string.nt_lyrics_title), Modifier.staggeredEnter(1))
    Row(
        Modifier.staggeredEnter(2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChipRow(sections, selectedSection, onSelectSection, Modifier.weight(1f, fill = false))
        NtIconButton(DotIcon.ADD, stringResource(R.string.nt_add_section), onAddSection)
    }
    NtCard(Modifier.heightIn(min = 200.dp).staggeredEnter(3)) {
        lines.forEach { NtBody(it) }
        Row(verticalAlignment = Alignment.CenterVertically) { NtCaret() }
    }
    NtCard(Modifier.staggeredEnter(4)) {
        NtLabel(stringResource(R.string.nt_rhymes_for, rhymeWord))
        ChipRow(rhymes, selected = -1, onSelect = {})
        if (rhymesLoading) NtSkeleton(Modifier.fillMaxWidth(0.6f))
    }
    Row(
        Modifier.fillMaxWidth().staggeredEnter(5),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NtPulsingLed(color = Nt.White)
            NtLabel(stringResource(R.string.nt_autosaved))
        }
        NtIconButton(DotIcon.PLAY, stringResource(R.string.nt_play), onPlay)
    }
    NtButton(
        stringResource(R.string.nt_next_visuals), onNext,
        Modifier.fillMaxWidth(), style = NtButtonStyle.Primary
    )
}

// ---------------------------------------------------------------- 3. Visuals

/** Square cover preview: drifting dot grid plus a pulsing red glow behind the title. */
@Composable
fun NtCoverArt(title: String, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "cover")
    val drift by transition.animateFloat(
        0f, 14f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "drift"
    )
    val glow by rememberPulse(periodMs = 3000)
    val shape = RoundedCornerShape(Nt.CardRadius)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Nt.Surface)
            .border(1.dp, Nt.Border, shape)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val step = 14.dp.toPx()
            val shift = if (reduced) 0f else drift.dp.toPx()
            var y = -step + shift
            while (y < size.height + step) {
                var x = -step + shift
                while (x < size.width + step) {
                    drawCircle(Nt.White.copy(alpha = 0.18f), 1.2.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Nt.Red.copy(alpha = 0.55f * glow), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.4f
                ),
                radius = size.minDimension * 0.4f,
                center = center
            )
        }
        Text(title, color = Nt.White, fontFamily = NothingDotMatrixFontFamily, fontSize = 64.sp)
    }
}

@Composable
fun ColumnScope.NtVisualsContent(
    artTitle: String,
    styles: List<String>,
    selectedStyle: Int,
    onSelectStyle: (Int) -> Unit,
    progress: Float?,
    onGenerate: () -> Unit
) {
    NtTitle(stringResource(R.string.nt_visuals_title), Modifier.staggeredEnter(1))
    NtCoverArt(artTitle, Modifier.staggeredEnter(2))
    ChipRow(styles, selectedStyle, onSelectStyle, Modifier.staggeredEnter(3))
    if (progress != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(stringResource(R.string.nt_rendering_cover))
            NtLabel("${(progress * 100).toInt()}%", color = Nt.RedText)
        }
        NtProgressBar(progress, stringResource(R.string.nt_cover_progress))
    }
    NtButton(
        stringResource(R.string.nt_generate_cover), onGenerate,
        Modifier.fillMaxWidth(), style = NtButtonStyle.Primary
    )
}

// ---------------------------------------------------------------- 4. Sync

@Composable
fun ColumnScope.NtSyncContent(
    fileName: String,
    duration: String,
    phaseLabel: String,
    progress: Float,
    steps: List<NtStep>,
    before: String,
    activeWord: String,
    after: String,
    onCancel: () -> Unit
) {
    NtTitle(stringResource(R.string.nt_sync_title), Modifier.staggeredEnter(1))
    NtCard(Modifier.staggeredEnter(2)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(fileName)
            NtLabel(duration)
        }
        NtEqualizer(count = 26)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        NtBody(phaseLabel)
        NtLabel("${(progress * 100).toInt()}%", color = Nt.RedText)
    }
    NtProgressBar(progress, stringResource(R.string.nt_sync_progress))
    NtStepList(steps, Modifier.staggeredEnter(4))
    NtCard(Modifier.staggeredEnter(5)) {
        NtLabel(stringResource(R.string.nt_estimated))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NtBody(before)
            NtWordChip(activeWord)
            NtBody(after)
        }
    }
    NtButton(stringResource(R.string.nt_cancel), onCancel, Modifier.fillMaxWidth())
}

// ---------------------------------------------------------------- 5. Export

@Composable
fun ColumnScope.NtExportContent(
    artist: String,
    previewBefore: String,
    previewWord: String,
    progress: Float,
    frameText: String,
    timeText: String,
    steps: List<NtStep>,
    showBackgroundNote: Boolean,
    canShare: Boolean,
    onCancel: () -> Unit,
    onShare: () -> Unit
) {
    NtTitle(stringResource(R.string.nt_export_title), Modifier.staggeredEnter(1))
    Row(Modifier.fillMaxWidth().staggeredEnter(2), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        NtCard(Modifier.width(128.dp).heightIn(min = 216.dp)) {
            NtLabel(artist, color = Nt.RedText)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NtBody(previewBefore)
            }
            NtWordChip(previewWord)
            NtEqualizer(count = 9, height = 28.dp)
        }
        Column(Modifier.weight(1f)) {
            NtLabel(stringResource(R.string.nt_progress))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${(progress * 100).toInt()}",
                    color = Nt.White, fontFamily = NothingDotMatrixFontFamily, fontSize = 52.sp
                )
                Text("%", color = Nt.RedText, fontFamily = NothingDotMatrixFontFamily, fontSize = 52.sp)
            }
            NtBody(frameText, color = Nt.Grey)
            NtBody(timeText, color = Nt.Grey)
        }
    }
    NtProgressBar(progress, stringResource(R.string.nt_export_progress))
    NtStepList(steps)
    if (showBackgroundNote) NtBody(stringResource(R.string.nt_background_note), color = Nt.Grey)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NtButton(stringResource(R.string.nt_cancel), onCancel, Modifier.weight(1f))
        NtButton(
            stringResource(R.string.nt_share), onShare, Modifier.weight(1f),
            style = NtButtonStyle.Primary, enabled = canShare
        )
    }
}

// ---------------------------------------------------------------- 6. Library

data class NtSong(val title: String, val subtitle: String)

@Composable
private fun CoverThumb(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        modifier
            .size(48.dp)
            .clip(shape)
            .background(Nt.Surface)
            .border(1.dp, Nt.Border, shape)
    ) {
        val step = 8.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(Nt.Red, 1.2.dp.toPx(), Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

@Composable
fun ColumnScope.NtLibraryContent(
    songs: List<NtSong>,
    playingIndex: Int?,
    onPlay: (Int) -> Unit
) {
    NtTitle(stringResource(R.string.nt_library_title), Modifier.staggeredEnter(1))
    NtCard(Modifier.staggeredEnter(2)) { NtBody(stringResource(R.string.nt_search_songs), color = Nt.Grey) }
    songs.forEachIndexed { index, song ->
        Row(
            Modifier.fillMaxWidth().staggeredEnter(3 + index),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CoverThumb()
            Column(Modifier.weight(1f)) {
                NtBody(song.title)
                NtBody(song.subtitle, color = Nt.Grey)
            }
            if (playingIndex == index) {
                NtEqualizer(count = 4, modifier = Modifier.width(30.dp), height = 24.dp)
            } else {
                NtIconButton(DotIcon.PLAY, stringResource(R.string.nt_play), { onPlay(index) })
            }
        }
    }
}

/** Compact player card shown above the bottom navigation (pass it as `bottomExtra`). */
@Composable
fun NtMiniPlayer(title: String, progress: Float, isPlaying: Boolean, onToggle: () -> Unit) {
    NtCard(Modifier.padding(horizontal = Nt.ScreenPadding, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NtBody(title)
                NtProgressBar(progress, stringResource(R.string.nt_library_title))
            }
            NtIconButton(
                if (isPlaying) DotIcon.PAUSE else DotIcon.PLAY,
                stringResource(if (isPlaying) R.string.nt_pause else R.string.nt_play),
                onToggle
            )
        }
    }
}

// ---------------------------------------------------------------- 7. Settings

@Composable
fun ColumnScope.NtSettingsContent(
    languages: List<String>,
    selectedLanguage: Int,
    onSelectLanguage: (Int) -> Unit,
    cloudTranscription: Boolean,
    onCloudChange: (Boolean) -> Unit,
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    qualities: List<String>,
    selectedQuality: Int,
    onSelectQuality: (Int) -> Unit,
    storageUsed: Float
) {
    NtTitle(stringResource(R.string.nt_settings_title), Modifier.staggeredEnter(1))
    NtCard(Modifier.staggeredEnter(2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(stringResource(R.string.nt_language))
            ChipRow(languages, selectedLanguage, onSelectLanguage)
        }
    }
    NtCard(Modifier.staggeredEnter(3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                NtBody(stringResource(R.string.nt_cloud))
                NtBody(stringResource(R.string.nt_cloud_hint), color = Nt.Grey)
            }
            NtSwitch(cloudTranscription, onCloudChange, stringResource(R.string.nt_cloud))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(stringResource(R.string.nt_haptics))
            NtSwitch(haptics, onHapticsChange, stringResource(R.string.nt_haptics))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(stringResource(R.string.nt_quality))
            ChipRow(qualities, selectedQuality, onSelectQuality)
        }
    }
    NtCard(Modifier.staggeredEnter(4)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NtBody(stringResource(R.string.nt_storage))
            NtLabel("${(storageUsed * 100).toInt()}%", color = Nt.RedText)
        }
        NtProgressBar(storageUsed, stringResource(R.string.nt_storage))
    }
}