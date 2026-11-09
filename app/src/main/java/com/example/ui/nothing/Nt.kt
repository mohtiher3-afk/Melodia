package com.example.ui.nothing

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Design tokens of the Nothing-style UI. Values match docs/NOTHING_UI_SPEC.md. */
object Nt {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0D0D0D)
    val Border = Color(0xFF2A2A2A)
    val Outline = Color(0xFF333333)
    val Track = Color(0xFF262626)
    val Grey = Color(0xFF9A9A9A)
    val White = Color(0xFFFFFFFF)
    val Red = Color(0xFFD71921)
    /** Use for SMALL red text: plain [Red] on black is only ~3.8:1. */
    val RedText = Color(0xFFFF4B52)
    val Pink = Color(0xFFFF7DB9)
    val CardRadius = 22.dp
    val ScreenPadding = 20.dp
}

/** True when the user turned animations off (Developer options / accessibility). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Fade in and rise 14dp, 500 ms, delayed by [index] * 50 ms. Plays once per composition. */
fun Modifier.staggeredEnter(index: Int): Modifier = composed {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(index * 50L)
            progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }
    Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/** 1 -> 0.35 -> 1 loop used by LEDs and "current step" rings. Static when [active] is false or motion is reduced. */
@Composable
fun rememberPulse(active: Boolean = true, periodMs: Int = 1600): State<Float> {
    val reduced = rememberReducedMotion()
    if (!active || reduced) return remember { mutableFloatStateOf(1f) }
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(periodMs / 2, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseValue"
    )
}