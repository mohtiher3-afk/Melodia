package com.example.ui.nothing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

/** 7x7 dot-matrix glyphs. '#' is a lit dot. Rows go top to bottom. */
enum class DotIcon(val rows: List<String>) {
    STUDIO(listOf("...#...", "..###..", ".#####.", "#######", ".#####.", "..###..", "...#...")),
    LYRICS(listOf("#######", ".......", "#####..", ".......", "#######", ".......", "####...")),
    VISUALS(listOf("#######", "#....##", "#.#...#", "##.#..#", "#.###.#", "#.....#", "#######")),
    SYNC(listOf("....#..", "#######", "....#..", ".......", "..#....", "#######", "..#....")),
    EXPORT(listOf("...#...", "..###..", ".#.#.#.", "...#...", "...#...", "#.....#", "#######")),
    LIBRARY(listOf("...####", "...#..#", "...#..#", "...#..#", "###..##", "###..##", ".#...#.")),
    SETTINGS(listOf(".#####.", "#.....#", "#..#..#", "#.###.#", "#..#..#", "#.....#", ".#####.")),
    PLAY(listOf("#......", "###....", "#####..", "#######", "#####..", "###....", "#......")),
    PAUSE(listOf(".##.##.", ".##.##.", ".##.##.", ".##.##.", ".##.##.", ".##.##.", ".##.##.")),
    CHECK(listOf(".......", "......#", ".....#.", "#...#..", ".#.#...", "..#....", ".......")),
    CLOSE(listOf("#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#")),
    ADD(listOf("...#...", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."))
}

/**
 * Dot-matrix icon. With [animated] a diagonal wave of pulses runs through the lit dots.
 * [contentDescription] == null marks the icon as decorative.
 */
@Composable
fun DotMatrixIcon(
    icon: DotIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    tint: Color = LocalContentColor.current,
    animated: Boolean = false
) {
    val reduced = rememberReducedMotion()
    val wave = if (animated && !reduced) {
        rememberInfiniteTransition(label = "dots").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "dotPhase"
        )
    } else null

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }

    Canvas(modifier.size(iconSize).then(semanticsModifier)) {
        val cell = this.size.width / 7f
        val radius = cell * 0.38f
        val phase = wave?.value ?: 0f
        icon.rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == '#') {
                    var alpha = 1f
                    var scale = 1f
                    if (wave != null) {
                        val p = (phase + (x + y) / 14f) % 1f
                        val w = (0.5f - 0.5f * cos(2.0 * PI * p).toFloat())
                        alpha = 1f - 0.7f * w
                        scale = 1f - 0.4f * w
                    }
                    drawCircle(
                        color = tint.copy(alpha = alpha),
                        radius = radius * scale,
                        center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
                    )
                }
            }
        }
    }
}