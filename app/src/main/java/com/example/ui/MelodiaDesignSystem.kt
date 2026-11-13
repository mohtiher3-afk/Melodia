package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CommitHaptic
import com.example.ui.components.TapHaptic
import com.example.ui.components.pressable
import com.example.ui.components.rememberPressScale
import com.example.ui.theme.MelodiaTheme
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingMutedGrey
import com.example.ui.theme.NothingOutline
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingTrack
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

object MelodiaTheme {
    val Background = NothingBlack
    val Surface = NothingDarkSurface
    val Border = NothingBorder
    val Track = NothingTrack
    val Accent = NothingRed
    val TextMain = NothingWhite
    val TextSecondary = NothingGrey
}

/** Shared spacing scale (4/8/12/16/24 + 20dp screen padding). */
object MelodiaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val screenPadding = 20.dp
}

/** Corner radii by role (card 22dp, chips and controls pill-shaped, thumbnails 12dp). */
object MelodiaRadius {
    val chip = 50.dp
    val control = 50.dp
    val card = 22.dp
    val thumbnail = 12.dp
}

/**
 * Nothing-style dot-matrix backdrop (20dp spacing, 1dp dots, white at 10% alpha over black).
 */
fun Modifier.dotGrid(
    spacing: Dp = 20.dp,
    dotRadius: Dp = 0.5.dp,
    color: Color = NothingWhite.copy(alpha = 0.10f)
): Modifier = drawWithCache {
    val step = spacing.toPx()
    val radiusPx = dotRadius.toPx()
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
    onDrawBehind {
        // drawPoints is available in DrawScope (inside Canvas/drawWithCache/onDrawBehind)
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = color,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** A restrained red ambient bloom that adds depth without competing with content. */
fun Modifier.ambientGlow(): Modifier = drawWithCache {
    val glow = Brush.radialGradient(
        colors = listOf(NothingRed.copy(alpha = 0.14f), Color.Transparent),
        center = Offset(size.width * 0.88f, size.height * 0.03f),
        radius = maxOf(size.width, size.height) * 0.78f
    )
    onDrawBehind { drawRect(glow) }
}

enum class MelodiaButtonStyle { Primary, Secondary, Quiet }

/**
 * Standard action button: pill-shaped, 48dp minimum touch target, whole-button press feedback.
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
    val shape = CircleShape
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            letterSpacing = 0.08.sp,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Standard Nothing-style Chip: fully rounded (pill), normal and selected (white fill, black text).
 */
@Composable
fun MelodiaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = CircleShape
    val containerColor = if (selected) NothingWhite else NothingCardSurface
    val contentColor = if (selected) NothingBlack else NothingWhite
    val borderColor = if (selected) NothingWhite else NothingBorder

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, enabled = enabled, pressedScale = 0.95f)

    Box(
        modifier = modifier
            .scale(pressScale)
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.08.sp,
            color = contentColor
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
            .clip(CircleShape)
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

// ---------------------------------------------------------------------------
// Previews for Task U1 Design Tokens and Components
// ---------------------------------------------------------------------------

@Preview(name = "Button Styles - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0xFF000000)
@Preview(name = "Button Styles - Arabic RTL", locale = "ar", showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MelodiaButtonPreview() {
    Column(
        modifier = Modifier
            .background(NothingBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MelodiaActionButton(text = "PRIMARY ACTION", onClick = {}, style = MelodiaButtonStyle.Primary)
        MelodiaActionButton(text = "SECONDARY ACTION", onClick = {}, style = MelodiaButtonStyle.Secondary)
        MelodiaActionButton(text = "QUIET ACTION", onClick = {}, style = MelodiaButtonStyle.Quiet)
        MelodiaActionButton(text = "إجراء رئيسي", onClick = {}, style = MelodiaButtonStyle.Primary)
    }
}

@Preview(name = "Card - Dark & Arabic", locale = "ar", fontScale = 1.5f, showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MelodiaCardPreview() {
    Box(
        modifier = Modifier
            .background(NothingBlack)
            .padding(16.dp)
    ) {
        MelodiaCard(headerTitle = "بطاقة العينات / SAMPLE CARD") {
            Text(
                text = "محتوى البطاقة بالتصميم النقطي والمعايير المحددة",
                fontFamily = SpaceMonoFontFamily,
                color = NothingWhite,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(name = "Chips Normal & Selected", showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MelodiaChipPreview() {
    Row(
        modifier = Modifier
            .background(NothingBlack)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MelodiaChip(text = "NORMAL CHIP", selected = false, onClick = {})
        MelodiaChip(text = "SELECTED CHIP", selected = true, onClick = {})
        MelodiaChip(text = "محدد", selected = true, onClick = {})
    }
}
