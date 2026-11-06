package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Genuine Nothing OS Dot Matrix typography using installed local font resource
val NothingDotMatrixFontFamily = FontFamily(
    Font(R.font.dotgothic16, FontWeight.Normal)
)

// Monospaced technical typography for code and metadata
val SpaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono, FontWeight.Normal)
)

// Pixel / Dot Matrix alternative
val SilkscreenFontFamily = FontFamily(
    Font(R.font.silkscreen, FontWeight.Normal)
)

val NothingTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 1.0.sp,
        color = HighContrastWhite
    ),
    displayMedium = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.8.sp,
        color = HighContrastWhite
    ),
    displaySmall = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    headlineLarge = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    headlineSmall = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    titleLarge = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastWhite
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        color = HighContrastOffWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.25.sp,
        color = HighContrastOffWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.25.sp,
        color = HighContrastGrey
    ),
    bodySmall = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp,
        color = HighContrastMuted
    ),
    labelLarge = TextStyle(
        fontFamily = NothingDotMatrixFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 1.0.sp,
        color = HighContrastWhite
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
        color = HighContrastOffWhite
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
        color = HighContrastGrey
    )
)
