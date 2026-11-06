package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Strict high-contrast monochrome palette with signature Nothing Red accents
 * for interactive elements, buttons, and state indicators.
 */
private val HighContrastNothingColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = HighContrastWhite,
    primaryContainer = NothingRedDim,
    onPrimaryContainer = HighContrastWhite,
    secondary = HighContrastWhite,
    onSecondary = HighContrastBlack,
    secondaryContainer = HighContrastElevated,
    onSecondaryContainer = HighContrastWhite,
    tertiary = NothingRed,
    onTertiary = HighContrastWhite,
    tertiaryContainer = NothingRedDim,
    onTertiaryContainer = HighContrastWhite,
    background = HighContrastBlack,
    onBackground = HighContrastWhite,
    surface = HighContrastDarkSurface,
    onSurface = HighContrastWhite,
    surfaceVariant = HighContrastCardSurface,
    onSurfaceVariant = HighContrastOffWhite,
    surfaceContainer = HighContrastCardSurface,
    surfaceContainerHigh = HighContrastElevated,
    outline = HighContrastBorder,
    outlineVariant = HighContrastOutline,
    error = NothingRed,
    onError = HighContrastWhite,
    errorContainer = NothingRedDim,
    onErrorContainer = HighContrastWhite
)

@Composable
fun MelodiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighContrastNothingColorScheme,
        typography = NothingTypography,
        content = content
    )
}
