package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Returns the active accent color adapted for small text: a slightly lighter
 * blend of the accent when rendered on a dark background and a slightly darker
 * blend on a light background, so small accent text keeps roughly WCAG AA
 * (~4.5:1) contrast.
 *
 * This replaces the legacy static tokens [NothingRedSmallText] and
 * `Nt.RedText`, which were tuned for the default Nothing Red only and did not
 * follow the user-selected accent color.
 */
@Composable
fun melodiaAccentText(): Color {
    val base = MaterialTheme.colorScheme.primary
    val onDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (onDarkBackground) {
        lerp(base, Color.White, 0.25f)
    } else {
        lerp(base, Color.Black, 0.30f)
    }
}
