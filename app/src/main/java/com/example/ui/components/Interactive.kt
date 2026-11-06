package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Gives any surface a physical press response: it dips under the finger and
 * springs back on release, with an optional haptic tick.
 *
 * The ripple is kept (via [LocalIndication]) so the feedback survives even when
 * the system animator scale is set to zero, and the scale animation is driven
 * by the same interaction source as the click so it can never get stuck.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    haptic: HapticFeedbackType? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = {
                haptic?.let { haptics.performHapticFeedback(it) }
                onClick()
            }
        )
}

/**
 * Press response for items that also need selection semantics (tabs, filters).
 * Keeps the caller's own `selectable`/`clickable` and only adds the motion.
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f
): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale_shared"
    )
    return scale
}

/** Light haptic tick used for taps that change state. */
val TapHaptic: HapticFeedbackType
    get() = HapticFeedbackType.TextHandleMove

/** Stronger confirmation used for actions that commit or destroy something. */
val CommitHaptic: HapticFeedbackType
    get() = HapticFeedbackType.LongPress
