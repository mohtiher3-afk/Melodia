package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import kotlin.math.sin

enum class DotIcon(val rows: List<String>) {
    STUDIO(
        listOf(
            "...#...",
            "..###..",
            ".#####.",
            "#######",
            ".#####.",
            "..###..",
            "...#..."
        )
    ),
    LYRICS(
        listOf(
            "#######",
            ".......",
            "#####..",
            ".......",
            "#######",
            ".......",
            "####..."
        )
    ),
    VISUALS(
        listOf(
            "#######",
            "#....##",
            "#.#...#",
            "##.#..#",
            "#.###.#",
            "#.....#",
            "#######"
        )
    ),
    SYNC(
        listOf(
            "....#..",
            "#######",
            "....#..",
            ".......",
            "..#....",
            "#######",
            "..#...."
        )
    ),
    EXPORT(
        listOf(
            "...#...",
            "..###..",
            ".#.#.#.",
            "...#...",
            "...#...",
            "#.....#",
            "#######"
        )
    ),
    LIBRARY(
        listOf(
            "...####",
            "...#..#",
            "...#..#",
            "...#..#",
            "###..##",
            "###..##",
            ".#...#."
        )
    ),
    SETTINGS(
        listOf(
            ".#####.",
            "#.....#",
            "#..#..#",
            "#.###.#",
            "#..#..#",
            "#.....#",
            ".#####."
        )
    ),
    PLAY(
        listOf(
            "#......",
            "###....",
            "#####..",
            "#######",
            "#####..",
            "###....",
            "#......"
        )
    ),
    PAUSE(
        listOf(
            ".##.##.",
            ".##.##.",
            ".##.##.",
            ".##.##.",
            ".##.##.",
            ".##.##.",
            ".##.##."
        )
    ),
    CHECK(
        listOf(
            ".......",
            "......#",
            ".....#.",
            "#...#..",
            ".#.#...",
            "..#....",
            "......."
        )
    ),
    CLOSE(
        listOf(
            "#.....#",
            ".#...#.",
            "..#.#..",
            "...#...",
            "..#.#..",
            ".#...#.",
            "#.....#"
        )
    ),
    ADD(
        listOf(
            "...#...",
            "...#...",
            "...#...",
            "#######",
            "...#...",
            "...#...",
            "...#..."
        )
    )
}

/**
 * Renders a 7x7 dot-matrix icon on Canvas.
 * When [animated] is true, dots pulse with a diagonal phase offset wave across the icon.
 */
@Composable
fun DotMatrixIcon(
    icon: DotIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = LocalContentColor.current,
    animated: Boolean = false
) {
    val semanticsModifier = if (contentDescription == null) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }

    val wavePhase = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "dot_matrix_wave")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
        phase
    } else 0f

    Canvas(
        modifier = modifier
            .size(size)
            .then(semanticsModifier)
    ) {
        val cellSize = size.toPx() / 7f
        val baseRadius = cellSize * 0.38f

        for (r in 0 until 7) {
            val rowStr = icon.rows.getOrNull(r) ?: continue
            for (c in 0 until 7) {
                if (c < rowStr.length && rowStr[c] == '#') {
                    val centerX = c * cellSize + cellSize / 2f
                    val centerY = r * cellSize + cellSize / 2f

                    val (dotAlpha, dotRadius) = if (animated) {
                        // Diagonal wave phase offset by (r + c)
                        val offset = (r + c) * 0.5f
                        val wave = (sin((wavePhase + offset).toDouble()).toFloat() + 1f) / 2f // 0f to 1f
                        val alpha = 0.3f + (0.7f * wave)
                        val radiusScale = 0.6f + (0.4f * wave)
                        alpha to (baseRadius * radiusScale)
                    } else {
                        1f to baseRadius
                    }

                    drawCircle(
                        color = tint.copy(alpha = tint.alpha * dotAlpha),
                        radius = dotRadius,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "All 12 DotIcons - Static & Animated", showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DotMatrixIconPreview() {
    Column(
        modifier = Modifier
            .background(NothingBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DOT-MATRIX ICONS (24DP, 28DP, 48DP)",
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = NothingWhite
        )

        DotIcon.entries.chunked(4).forEach { rowIcons ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowIcons.forEach { dotIcon ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DotMatrixIcon(
                            icon = dotIcon,
                            contentDescription = dotIcon.name,
                            size = 28.dp,
                            tint = NothingWhite,
                            animated = true
                        )
                        Text(
                            text = dotIcon.name,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 9.sp,
                            color = NothingWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
