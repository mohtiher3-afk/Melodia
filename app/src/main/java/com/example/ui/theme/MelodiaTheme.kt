package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Strict high-contrast monochrome palette with signature Nothing Red accents
 * for interactive elements, buttons, and state indicators.
 * DARK ONLY - Pure black background (#000000)
 */

// Color definitions
val NothingBlack = Color(0xFF000000)
val NothingWhite = Color(0xFFFFFFFF)
val NothingOffWhite = Color(0xFFEBEBEB)
val NothingGrey = Color(0xFF9A9A9A)
val NothingMutedGrey = Color(0xFF6E6E73)
val NothingDarkGrey = Color(0xFF262626)
val NothingRed = Color(0xFFD71921)
val NothingRedDim = Color(0xFF38080A)
val NothingRedHover = Color(0xFFEB222B)
val NothingRedGlow = Color(0x33D71921)
val NothingRedSmallText = Color(0xFFFF4B52)
val NothingCardSurface = Color(0xFF0D0D0D)
val NothingElevatedSurface = Color(0xFF1A1A1A)
val NothingBorder = Color(0xFF2A2A2A)
val NothingOutline = Color(0xFF333333)
val NothingTrack = Color(0xFF262626)
val NothingDarkSurface = NothingCardSurface

private val MelodiaDarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    primaryContainer = NothingRedDim,
    onPrimaryContainer = NothingWhite,
    secondary = NothingWhite,
    onSecondary = NothingBlack,
    secondaryContainer = NothingElevatedSurface,
    onSecondaryContainer = NothingWhite,
    tertiary = NothingRed,
    onTertiary = NothingWhite,
    tertiaryContainer = NothingRedDim,
    onTertiaryContainer = NothingWhite,
    background = NothingBlack,       // Pure black #000000
    onBackground = NothingWhite,
    surface = NothingCardSurface,    // #0D0D0D
    onSurface = NothingWhite,
    surfaceVariant = NothingCardSurface,
    onSurfaceVariant = NothingOffWhite,
    surfaceContainer = NothingCardSurface,
    surfaceContainerHigh = NothingElevatedSurface,
    outline = NothingBorder,         // #2A2A2A
    outlineVariant = NothingOutline,
    error = NothingRed,
    onError = NothingWhite,
    errorContainer = NothingRedDim,
    onErrorContainer = NothingWhite
)

@Composable
fun MelodiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MelodiaDarkColorScheme,
        typography = NothingTypography,
        content = content
    )
}

/**
 * Theme colors object for direct access in components
 */
object MelodiaColors {
    val Background = NothingBlack        // #000000
    val Card = NothingCardSurface        // #0D0D0D
    val Elevated = NothingElevatedSurface // #1A1A1A
    val Border = NothingBorder           // #2A2A2A
    val Track = NothingTrack             // #262626
    val Accent = NothingRed              // #D71921
    val AccentDim = NothingRedDim        // #38080A
    val AccentHover = NothingRedHover    // #EB222B
    val AccentGlow = NothingRedGlow      // #33D71921
    val AccentSmallText = NothingRedSmallText // #FF4B52 (4.5:1 contrast)
    val TextMain = NothingWhite          // #FFFFFF
    val TextOffWhite = NothingOffWhite   // #EBEBEB
    val TextSecondary = NothingGrey      // #9A9A9A
    val TextMuted = NothingMutedGrey     // #6E6E73
    val TextDark = NothingDarkGrey       // #262626
}