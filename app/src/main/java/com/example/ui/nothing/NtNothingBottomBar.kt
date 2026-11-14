package com.example.ui.nothing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DotIcon
import com.example.ui.components.DotMatrixIcon
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingElevatedSurface
import com.example.ui.theme.NothingMutedGrey
import com.example.ui.theme.NothingOutline
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import androidx.compose.material3.MaterialTheme

/**
 * Nothing OS styled bottom navigation bar for the three main destinations:
 * PROJECT, LIBRARY, SETTINGS.
 * 64dp height, 1dp top line #262626, selected tab has 2dp red top indicator.
 */
@Composable
fun NtNothingBottomBar(
    selectedDestination: NtDestination,
    onDestinationSelected: (NtDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nt_nothing_bottom_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1dp top line #262626
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Nt.Track))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .background(Nt.Surface)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NtDestination.entries.forEach { destination ->
                    NtBottomNavItem(
                        destination = destination,
                        selected = selectedDestination == destination,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NtBottomNavItem(
    destination: NtDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) Nt.White else Nt.Grey,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_nav_item_color"
    )
    val indicatorVisible by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "bottom_nav_indicator_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.9f)
    val haptics = LocalHapticFeedback.current

    // Map NtDestination to DotIcon
    val dotIcon = when (destination) {
        NtDestination.PROJECT -> DotIcon.STUDIO
        NtDestination.LIBRARY -> DotIcon.LIBRARY
        NtDestination.SETTINGS -> DotIcon.SETTINGS
    }

    // Map NtDestination to label string resource
    val labelRes = when (destination) {
        NtDestination.PROJECT -> com.example.R.string.nt_nav_project
        NtDestination.LIBRARY -> com.example.R.string.nt_nav_library
        NtDestination.SETTINGS -> com.example.R.string.nt_nav_settings
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .scale(pressScale)
            .selectable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                selected = selected,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.Tab
            )
            .testTag(destination.testTag)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 2dp red top indicator for selected tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Nt.Red)
                .alpha(indicatorVisible)
        )

        Spacer(modifier = Modifier.height(6.dp))

        DotMatrixIcon(
            icon = dotIcon,
            contentDescription = null,
            size = 24.dp,
            tint = contentColor,
            animated = selected
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(labelRes),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible
        )
    }
}

/**
 * Helper to animate scale on press.
 */
@Composable
private fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    return if (pressed) pressedScale else 1f
}