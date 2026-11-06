package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.KaraokeLyrics
import com.example.network.WordTiming
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

/** One rendered lyric line, already aligned against the word timings. */
private data class KaraokeLine(
    val words: List<WordTiming>,
    val sectionLabel: String? = null
) {
    val startTimeMs: Long get() = words.firstOrNull()?.startTimeMs ?: 0L
    val endTimeMs: Long get() = words.lastOrNull()?.endTimeMs ?: 0L
}

/**
 * Distributes the flat [KaraokeLyrics.wordTimings] list across the lines of the
 * lyric text so each line knows which words belong to it.
 *
 * Lines that only contain a section marker (e.g. `[Chorus]`) are kept as labels
 * and carry no timings.
 */
private fun alignLines(karaoke: KaraokeLyrics): List<KaraokeLine> {
    val rawLines = karaoke.lyrics.lines().filter { it.isNotBlank() }
    if (rawLines.isEmpty()) {
        return listOf(KaraokeLine(words = karaoke.wordTimings))
    }

    val lines = mutableListOf<KaraokeLine>()
    var cursor = 0
    rawLines.forEach { raw ->
        val trimmed = raw.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            lines += KaraokeLine(words = emptyList(), sectionLabel = trimmed)
            return@forEach
        }
        val tokenCount = trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
        val slice = karaoke.wordTimings.drop(cursor).take(tokenCount)
        cursor += slice.size
        if (slice.isNotEmpty()) lines += KaraokeLine(words = slice)
    }

    // If the lyric text and the timings disagree, fall back to the timings alone
    // so the karaoke never shows an empty stage.
    if (lines.none { it.words.isNotEmpty() }) {
        return listOf(KaraokeLine(words = karaoke.wordTimings))
    }
    return lines
}

@Composable
fun KaraokeLyricsView(
    karaokeLyrics: KaraokeLyrics?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    if (karaokeLyrics == null) return

    val lines = remember(karaokeLyrics) { alignLines(karaokeLyrics) }
    val listState = rememberLazyListState()

    val activeLineIndex = remember(lines, currentPositionMs) {
        val exact = lines.indexOfFirst { line ->
            line.words.any { currentPositionMs in it.startTimeMs..it.endTimeMs }
        }
        if (exact != -1) {
            exact
        } else {
            // Between words (or before the first one): hold the last line that
            // already started so the stage does not flicker back to the top.
            lines.indexOfLast { it.words.isNotEmpty() && it.startTimeMs <= currentPositionMs }
                .takeIf { it >= 0 } ?: 0
        }
    }

    LaunchedEffect(activeLineIndex) {
        if (lines.isNotEmpty()) {
            val offset = -maxLines / 3
            listState.animateScrollToItem(
                index = activeLineIndex.coerceIn(0, lines.lastIndex),
                scrollOffset = offset
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NothingBlack.copy(alpha = 0.9f))
            .padding(vertical = 12.dp)
            .testTag("karaoke_lyrics_view")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height((maxLines * 46).dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val isActive = index == activeLineIndex
                val lineAlpha by animateColorAsState(
                    targetValue = when {
                        isActive -> NothingWhite
                        line.startTimeMs < lines.getOrNull(activeLineIndex)?.startTimeMs ?: 0L ->
                            NothingGrey.copy(alpha = 0.55f)
                        else -> NothingGrey.copy(alpha = 0.75f)
                    },
                    animationSpec = tween(220),
                    label = "karaoke_line_tint"
                )
                val barHeight by animateDpAsState(
                    targetValue = if (isActive) 28.dp else 10.dp,
                    animationSpec = tween(200),
                    label = "karaoke_bar_height"
                )
                val fontSize by animateFloatAsState(
                    targetValue = if (isActive) 19f else 15f,
                    animationSpec = tween(200),
                    label = "karaoke_font_size"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active marker bar: grows in instead of popping.
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) NothingRed else Color.Transparent
                            )
                    )
                    Text(
                        text = lineText(line, currentPositionMs, lineAlpha),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = 26.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }

        // Tiny position readout keeps the view legible as a karaoke surface.
        Text(
            text = formatPosition(currentPositionMs),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 9.sp,
            color = NothingBorder,
            modifier = Modifier.padding(start = 25.dp, top = 8.dp)
        )
    }
}

@Composable
private fun lineText(
    line: KaraokeLine,
    currentPositionMs: Long,
    baseColor: Color
): AnnotatedString {
    line.sectionLabel?.let { label ->
        return buildAnnotatedString {
            withStyle(SpanStyle(color = NothingRed, fontWeight = FontWeight.Bold)) {
                append(label)
            }
        }
    }

    return buildAnnotatedString {
        line.words.forEachIndexed { wordIndex, word ->
            val isActive = currentPositionMs in word.startTimeMs..word.endTimeMs
            val isPast = currentPositionMs > word.endTimeMs
            val style = when {
                isActive -> SpanStyle(
                    color = NothingRed,
                    fontWeight = FontWeight.Bold,
                    background = NothingRed.copy(alpha = 0.18f)
                )
                isPast -> SpanStyle(color = NothingGrey.copy(alpha = 0.6f))
                else -> SpanStyle(color = baseColor)
            }
            if (wordIndex > 0) append(" ")
            withStyle(style) { append(word.text) }
        }
    }
}

private fun formatPosition(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
