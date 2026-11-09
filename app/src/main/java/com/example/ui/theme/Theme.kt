package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.RED,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accentColor = accent.color
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.24f),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFEBEBEB),
            onSecondary = Color(0xFF171717),
            background = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF0D0D0D),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF1A1A1A),
            onSurfaceVariant = Color(0xFFB8B8B8),
            outline = Color(0xFF3A3A3A),
            error = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF201010),
            secondary = Color(0xFF333333),
            onSecondary = Color.White,
            background = Color(0xFFF5F5F5),
            onBackground = Color(0xFF171717),
            surface = Color.White,
            onSurface = Color(0xFF171717),
            surfaceVariant = Color(0xFFEAEAEA),
            onSurfaceVariant = Color(0xFF555555),
            outline = Color(0xFFBDBDBD),
            error = Color(0xFFB3261E)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NothingTypography,
        content = content
    )
}
