package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Available theme modes for the application.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Available accent colors for the application.
 * Each accent color provides a [Color] value for use in the theme.
 */
enum class AccentColor(val color: Color) {
    RED(Color(0xFFD71921)),
    PINK(Color(0xFFFF7DB9)),
    ORANGE(Color(0xFFFF9F0A)),
    YELLOW(Color(0xFFFFD600)),
    GREEN(Color(0xFF00E676)),
    CYAN(Color(0xFF00E5FF)),
    BLUE(Color(0xFF2979FF)),
    PURPLE(Color(0xFFD500F9));

    companion object {
        val DEFAULT = RED
    }
}

/**
 * Represents the complete appearance configuration of the app.
 * Contains theme mode and accent color preferences.
 */
@Serializable
data class AppAppearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.DEFAULT
)