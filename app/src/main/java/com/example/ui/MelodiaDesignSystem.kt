package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TapHaptic
import com.example.ui.components.pressable
import com.example.ui.theme.*

object MelodiaTheme {
    val Background = NothingBlack
    val Surface = NothingCardSurface
    val Border = NothingBorder
    val Accent = NothingRed
    val TextMain = NothingWhite
    val TextSecondary = NothingGrey
}

@Composable
fun MelodiaCard(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MelodiaTheme.Surface),
        border = BorderStroke(1.dp, MelodiaTheme.Border)
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
            )
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
