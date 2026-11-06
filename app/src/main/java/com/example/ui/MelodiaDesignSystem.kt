package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CommitHaptic
import com.example.ui.components.TapHaptic
import com.example.ui.components.pressable
import com.example.ui.components.rememberPressScale
import com.example.ui.theme.*

object MelodiaTheme {
    val Background = NothingBlack
    val Surface = NothingCardSurface
    val Border = NothingBorder
    val Accent = NothingRed
    val TextMain = NothingWhite
    val TextSecondary = NothingGrey
}

/** Shared spacing scale. Screens should pick from these instead of ad-hoc dp values. */
object MelodiaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Corner radii by role, so controls, cards and chips read as a consistent family. */
object MelodiaRadius {
    val chip = 10.dp
    val control = 14.dp
    val card = 18.dp
}

/**
 * Nothing-style dot-matrix backdrop. Dots are densest at the top of the screen and fade out
 * towards the bottom, so the working area stays calm. Positions are computed once per size.
 */
fun Modifier.dotGrid(
    spacing: Dp = 22.dp,
    dotRadius: Dp = 0.9.dp,
    color: Color = NothingWhite
): Modifier = drawWithCache {
    val step = spacing.toPx()
    val bandAlphas = floatArrayOf(0.10f, 0.055f, 0.025f)
    val bands = Array(bandAlphas.size) { ArrayList<Offset>() }
    if (size.width > 0f && size.height > 0f && step > 0f) {
        val cols = (size.width / step).toInt() + 1
        val rows = (size.height / step).toInt() + 1
        for (r in 0 until rows) {
            val y = r * step + step / 2f
            val band = ((y / size.height) * bandAlphas.size).toInt().coerceIn(0, bandAlphas.size - 1)
            for (c in 0 until cols) {
                bands[band].add(Offset(c * step + step / 2f, y))
            }
        }
    }
    val strokeWidth = dotRadius.toPx() * 2f
    onDrawBehind {
        for (i in bands.indices) {
            drawPoints(
                points = bands[i],
                pointMode = PointMode.Points,
                color = color.copy(alpha = bandAlphas[i]),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

enum class MelodiaButtonStyle { Primary, Secondary, Quiet }

/**
 * Standard action button: 48 dp minimum touch target, whole-button press feedback, rounded
 * ripple, optional leading icon. Use [MelodiaButtonStyle.Primary] once per screen area.
 */
@Composable
fun MelodiaActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: MelodiaButtonStyle = MelodiaButtonStyle.Secondary,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(MelodiaRadius.control)
    val container = when {
        !enabled -> NothingDarkGrey
        style == MelodiaButtonStyle.Primary -> NothingRed
        style == MelodiaButtonStyle.Secondary -> NothingCardSurface
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> NothingMutedGrey
        style == MelodiaButtonStyle.Quiet -> NothingGrey
        else -> NothingWhite
    }
    val borderColor = if (style == MelodiaButtonStyle.Secondary && enabled) NothingBorder else Color.Transparent

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, enabled = enabled, pressedScale = 0.96f)
    val haptics = LocalHapticFeedback.current
    val haptic = if (style == MelodiaButtonStyle.Primary) CommitHaptic else TapHaptic

    Row(
        modifier = modifier
            .scale(pressScale)
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
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
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MelodiaCard(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MelodiaRadius.card),
        colors = CardDefaults.cardColors(containerColor = MelodiaTheme.Surface),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(NothingOutline, MelodiaTheme.Border))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (headerTitle != null) {
                Text(
                    text = headerTitle.uppercase(),
                    fontFamily = NothingDotMatrixFontFamily,
                    color = MelodiaTheme.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun MelodiaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) MelodiaTheme.Accent else NothingDarkGrey)
            .pressable(
                enabled = enabled,
                pressedScale = 0.94f,
                haptic = TapHaptic,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (enabled) Color.White else MelodiaTheme.TextSecondary
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status.uppercase()) {
        "READY", "DONE", "COMPLETED" -> NothingWhite
        "PROCESSING", "PENDING", "IDLE" -> MelodiaTheme.TextSecondary
        "FAILED", "ERROR" -> NothingRed
        else -> MelodiaTheme.TextSecondary
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.uppercase(),
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
