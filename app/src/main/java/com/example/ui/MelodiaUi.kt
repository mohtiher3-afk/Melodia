package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode as ComposeRepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DotoFontFamily
import com.example.ui.theme.MelodiaColors
import com.example.ui.theme.SpaceMonoFontFamily
import kotlinx.coroutines.delay
import java.util.ArrayList

// ─── Colors ────────────────────────────────────────────────────────────────

val MelodiaBlack = MelodiaColors.Background
val MelodiaCard = MelodiaColors.Card
val MelodiaRed = MelodiaColors.Accent
val MelodiaRedHi = MelodiaColors.AccentSmallText
val MelodiaWhite = MelodiaColors.TextMain
val MelodiaGrey = MelodiaColors.TextSecondary
val MelodiaLine = MelodiaColors.Border
val MelodiaTrack = MelodiaColors.Track

// ─── Fonts ──────────────────────────────────────────────────────────────────

val Mono = SpaceMonoFontFamily
val Doto = DotoFontFamily

// ─── Dot Grid Modifier ──────────────────────────────────────────────────────

/**
 * Subtle white dot grid (20dp spacing, ~10% alpha) using drawWithCache.
 * Apply to main screen background only.
 */
fun Modifier.dotGrid(
    spacing: Int = 20,
    dotRadius: Float = 0.5f,
    alpha: Float = 0.10f
): Modifier = drawWithCache {
    onDrawBehind {
        val step = spacing.dp.toPx()
        val radiusPx = dotRadius.dp.toPx()
        val points = ArrayList<Offset>()
        if (size.width > 0f && size.height > 0f && step > 0f) {
            val cols = (size.width / step).toInt() + 1
            val rows = (size.height / step).toInt() + 1
            for (r in 0 until rows) {
                val y = r * step + step / 2f
                for (c in 0 until cols) {
                    points.add(Offset(c * step + step / 2f, y))
                }
            }
        }
        val strokeWidth = radiusPx * 2f
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = MelodiaWhite.copy(alpha = alpha),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// ─── Nothing Button ────────────────────────────────────────────────────────

enum class NothingButtonStyle { Primary, Secondary, Quiet, IconOnly }

@Composable
fun NothingButton(
    text: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    style: NothingButtonStyle = NothingButtonStyle.Secondary,
    enabled: Boolean = true,
    testTag: String = "",
    content: @Composable () -> Unit = {}
) {
    // Icon-only variant: 48dp circle with 1dp border
    if (style == NothingButtonStyle.IconOnly) {
        val container = if (!enabled) MelodiaLine.copy(alpha = 0.3f) else MelodiaCard
        val contentColor = if (!enabled) MelodiaGrey.copy(alpha = 0.5f) else MelodiaWhite
        val borderColor = if (!enabled) MelodiaLine.copy(alpha = 0.3f) else MelodiaLine

        val interactionSource = remember { MutableInteractionSource() }
        val pressScale = rememberPressScale(interactionSource, enabled = enabled, pressedScale = 0.96f)
        val haptics = LocalHapticFeedback.current

        val btnModifier = if (testTag.isNotEmpty()) modifier.testTag(testTag) else modifier
        Box(
            modifier = btnModifier
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .size(48.dp)
                .clip(CircleShape)
                .background(container)
                .border(1.dp, borderColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                        haptics.performHapticFeedback(TapHaptic)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconColor = iconTint ?: contentColor
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        return
    }

    val shape = CircleShape
    val container = when {
        !enabled -> MelodiaLine.copy(alpha = 0.3f)
        style == NothingButtonStyle.Primary -> MelodiaRed
        style == NothingButtonStyle.Secondary -> MelodiaCard
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> MelodiaGrey.copy(alpha = 0.5f)
        style == NothingButtonStyle.Quiet -> MelodiaGrey
        else -> MelodiaWhite
    }
    val borderColor = if (style == NothingButtonStyle.Secondary && enabled) MelodiaLine else Color.Transparent

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, enabled = enabled, pressedScale = 0.96f)
    val haptics = LocalHapticFeedback.current
    val haptic = if (style == NothingButtonStyle.Primary) CommitHaptic else TapHaptic

    val btnModifier = if (testTag.isNotEmpty()) modifier.testTag(testTag) else modifier
    Row(
        modifier = btnModifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(haptic)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            val iconColor = iconTint ?: contentColor
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        content()
    }
}

// ─── Melodia Island ────────────────────────────────────────────────────────

@Composable
fun MelodiaIsland(
    state: IslandState = IslandState.Collapsed,
    onStateChange: (IslandState) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedHeight = remember { mutableStateOf(56f) }
    val targetHeight = when (state) {
        IslandState.Collapsed -> with(LocalDensity.current) { 56.dp.toPx() }
        IslandState.Expanded -> with(LocalDensity.current) { 120.dp.toPx() }
        IslandState.Hidden -> 0f
    }

    // Animate height
    val currentHeight by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = tween(300),
        label = "island_height"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MelodiaCard)
            .border(1.dp, MelodiaLine, RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}

/**
 * Task-progress island: compact title + progress bar, tap to expand, auto-collapse
 * after 4s. Renders nothing when [uiState] is Idle.
 * Also handles Playing state with equalizer bars + track title.
 */
@Composable
fun MelodiaTaskIsland(
    uiState: IslandUiState,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.status == IslandStatus.Idle) return

    val isDone = uiState.status == IslandStatus.Done
    val isFailed = uiState.status == IslandStatus.Failed
    val isPlaying = uiState.status == IslandStatus.Playing
    val accent = when {
        isDone -> MelodiaWhite
        isFailed -> MelodiaRed
        else -> MelodiaRed
    }
    val label = when (uiState.status) {
        IslandStatus.Done -> "DONE"
        IslandStatus.Failed -> "FAILED"
        IslandStatus.Running -> "RUNNING"
        IslandStatus.Playing -> "NOW PLAYING"
        IslandStatus.Idle -> ""
    }

    // Equalizer bars for Playing state
    val equalizerBars = remember {
        (0..5).map { mutableStateOf(0.3f + (Math.random() * 0.7).toFloat()) }
    }

    // Animate equalizer when playing
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                equalizerBars.forEach { bar ->
                    bar.value = 0.2f + (Math.random() * 0.8).toFloat()
                }
                delay(120)
            }
        } else {
            equalizerBars.forEach { bar ->
                bar.value = 0.1f
            }
        }
    }

    MelodiaIsland(
        state = uiState.state,
        onStateChange = { onToggle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.title.uppercase(),
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.08.sp,
                    color = MelodiaWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isPlaying) {
                    // Mini equalizer in the island
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        equalizerBars.forEach { bar ->
                            val animatedHeight = animateFloatAsState(
                                targetValue = bar.value,
                                animationSpec = androidx.compose.animation.core.tween(80),
                                label = "eq_bar"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(8.dp * animatedHeight.value)
                                    .background(MelodiaRed)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                                    .align(Alignment.Bottom)
                            )
                        }
                    }
                } else {
                    Text(
                        text = label,
                        fontFamily = Mono,
                        fontSize = 9.sp,
                        letterSpacing = 0.12.sp,
                        color = accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MelodiaLine)
            ) {
                val fill = when {
                    isDone || isFailed -> 1f
                    uiState.progress < 0f -> null
                    else -> uiState.progress.coerceIn(0f, 1f)
                }
                if (fill != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fill)
                            .background(accent)
                            .clip(RoundedCornerShape(2.dp))
                    )
                } else {
                    // Indeterminate sweep for running tasks
                    val sweep by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "island_progress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(sweep)
                            .background(accent)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ─── Melodia Splash ────────────────────────────────────────────────────────

@Composable
fun MelodiaSplash(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(2000, 500),
        label = "splash_progress"
    )
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, 2500),
        label = "splash_alpha"
    )
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = ComposeRepeatMode.Reverse
        ),
        label = "splash_pulse"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MelodiaBlack)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "MELODIA",
                fontFamily = Doto,
                fontWeight = FontWeight.Black,
                fontSize = 48.sp,
                letterSpacing = 4.sp,
                color = MelodiaWhite.copy(alpha = pulse * 0.3f + 0.7f)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Bar(progress = progress)
        }
    }
}

// ─── Progress Bar ──────────────────────────────────────────────────────────

@Composable
fun Bar(
    progress: Float,
    modifier: Modifier = Modifier.fillMaxWidth().width(200.dp)
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MelodiaLine)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(MelodiaRed)
                .clip(RoundedCornerShape(1.dp))
        )
    }
}

// ─── Pulse Dot ─────────────────────────────────────────────────────────────

@Composable
fun PulseDot(
    modifier: Modifier = Modifier.size(8.dp),
    color: Color = MelodiaRed,
    speed: Int = 1000
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(speed, delayMillis = 0, easing = LinearEasing),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(color, CircleShape)
    )
}

// ─── Helpers ────────────────────────────────────────────────────────────────

@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: HapticFeedbackType = TapHaptic,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, enabled = enabled, pressedScale = pressedScale)
    val haptics = LocalHapticFeedback.current
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = Role.Button,
            onClick = {
                haptics.performHapticFeedback(haptic)
                onClick()
            }
        )
}

val TapHaptic = HapticFeedbackType.TextHandleMove
val CommitHaptic = HapticFeedbackType.LongPress

@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f
): Float {
    val pressed = interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (enabled && pressed.value) pressedScale else 1f,
        animationSpec = tween(120),
        label = "press_scale"
    ).value
}