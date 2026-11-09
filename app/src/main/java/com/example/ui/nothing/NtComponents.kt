package com.example.ui.nothing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.SpaceMonoFontFamily

// ---------------------------------------------------------------- text

@Composable
fun NtTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Nt.White,
        fontFamily = NothingDotMatrixFontFamily,
        fontSize = 34.sp,
        lineHeight = 38.sp
    )
}

/** Small all-caps Space Mono label. */
@Composable
fun NtLabel(text: String, modifier: Modifier = Modifier, color: Color = Nt.Grey) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em
    )
}

@Composable
fun NtBody(text: String, modifier: Modifier = Modifier, color: Color = Nt.White) {
    Text(text = text, modifier = modifier, color = color, fontSize = 15.sp, lineHeight = 22.sp)
}

// ---------------------------------------------------------------- surfaces and controls

@Composable
fun NtCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Nt.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nt.Surface)
            .border(1.dp, Nt.Border, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

enum class NtButtonStyle { Primary, Secondary }

@Composable
fun NtButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NtButtonStyle = NtButtonStyle.Secondary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(50)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(150), label = "press")
    val background = when {
        !enabled -> Nt.Track
        style == NtButtonStyle.Primary -> Nt.Red
        else -> Nt.Surface
    }
    val borderColor = if (enabled && style == NtButtonStyle.Primary) Nt.Red else Nt.Outline
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = if (enabled) Nt.White else Nt.Grey,
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.06.em,
            maxLines = 1
        )
    }
}

@Composable
fun NtIconButton(
    icon: DotIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Nt.Surface)
            .border(1.dp, Nt.Outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DotMatrixIcon(icon, contentDescription, iconSize = 22.dp, tint = Nt.White, animated = animated)
    }
}

/** Pill chip. Selected = white fill with black text. */
@Composable
fun NtChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(if (selected) Nt.White else Color.Transparent)
            .border(1.dp, if (selected) Nt.White else Nt.Outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = if (selected) Nt.Black else Nt.White, fontSize = 13.sp, maxLines = 1)
    }
}

/** 52x30dp switch, red when on. */
@Composable
fun NtSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val track by animateColorAsState(if (checked) Nt.Red else Nt.Track, tween(300), label = "track")
    val knob = animateDpAsState(if (checked) 25.dp else 3.dp, tween(300), label = "knob")
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = label }
    ) {
        Box(
            Modifier
                .offset { IntOffset(knob.value.roundToPx(), 3.dp.roundToPx()) }
                .size(24.dp)
                .background(Nt.White, CircleShape)
        )
    }
}

// ---------------------------------------------------------------- waiting and progress

@Composable
fun NtPulsingLed(modifier: Modifier = Modifier, color: Color = Nt.Red) {
    val pulse by rememberPulse()
    Box(
        modifier
            .size(8.dp)
            .graphicsLayer {
                alpha = pulse
                scaleX = 0.8f + 0.2f * pulse
                scaleY = 0.8f + 0.2f * pulse
            }
            .background(color, CircleShape)
    )
}

/** Blinking text caret. */
@Composable
fun NtCaret(modifier: Modifier = Modifier) {
    val pulse by rememberPulse(periodMs = 900)
    Box(
        modifier
            .padding(start = 3.dp)
            .size(width = 2.dp, height = 16.dp)
            .graphicsLayer { alpha = if (pulse > 0.67f) 1f else 0f }
            .background(Nt.RedText)
    )
}

/**
 * 8dp progress bar. [progress] in 0..1 = determinate (moving diagonal stripes);
 * null = indeterminate (a 35% bar sweeping across).
 */
@Composable
fun NtProgressBar(progress: Float?, label: String, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "bar")
    val stripe by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart), label = "stripe"
    )
    val sweep by transition.animateFloat(
        -0.35f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "sweep"
    )
    val period = with(LocalDensity.current) { 40.dp.toPx() }
    val band = with(LocalDensity.current) { 20.dp.toPx() }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clipToBounds()
            .semantics {
                contentDescription = label
                if (progress != null) {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
                }
            }
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(Nt.Track, cornerRadius = radius)
        val start: Float
        val width: Float
        if (progress != null) {
            start = 0f
            width = size.width * progress.coerceIn(0f, 1f)
        } else {
            width = size.width * 0.35f
            start = size.width * (if (reduced) 0.3f else sweep)
        }
        if (width > 0f) {
            val shift = if (reduced) 0f else stripe * period
            val brush = Brush.linearGradient(
                0f to Nt.Red, 0.5f to Nt.Red, 0.5f to Nt.RedText, 1f to Nt.RedText,
                start = Offset(shift, 0f),
                end = Offset(shift + band, band),
                tileMode = TileMode.Repeated
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(start, 0f),
                size = Size(width, size.height),
                cornerRadius = radius
            )
        }
    }
}

/** Shimmering placeholder line. Give it a width via [modifier]. */
@Composable
fun NtSkeleton(modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "skeleton")
    val x by transition.animateFloat(
        -400f, 1200f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x"
    )
    val shift = if (reduced) 0f else x
    val brush = Brush.linearGradient(
        listOf(Color(0xFF1C1C1C), Color(0xFF3A3A3A), Color(0xFF1C1C1C)),
        start = Offset(shift, 0f),
        end = Offset(shift + 400f, 0f)
    )
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(brush))
}

/** Equalizer bars; each bar breathes between 100% and 25% height. */
@Composable
fun NtEqualizer(count: Int, modifier: Modifier = Modifier, height: Dp = 64.dp, active: Boolean = true) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "eq")
    val scales = List(count) { i ->
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                tween(if (i % 4 == 0) 700 else 500, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
                initialStartOffset = StartOffset((i % 3) * 120)
            ),
            label = "bar$i"
        )
    }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val slot = size.width / count
        val barWidth = minOf(6.dp.toPx(), slot * 0.6f)
        for (i in 0 until count) {
            val s = if (!active || reduced) 0.6f else scales[i].value
            val h = size.height * s
            drawRoundRect(
                color = if (i % 3 == 0) Nt.RedText else Nt.Red,
                topLeft = Offset(slot * i + (slot - barWidth) / 2f, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

enum class NtStepState { DONE, NOW, TODO }
data class NtStep(val label: String, val state: NtStepState)

@Composable
fun NtStepList(steps: List<NtStep>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEach { step ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val pulse by rememberPulse(active = step.state == NtStepState.NOW)
                Box(
                    Modifier
                        .size(18.dp)
                        .graphicsLayer { alpha = pulse }
                        .then(
                            when (step.state) {
                                NtStepState.DONE -> Modifier.background(Nt.Red, CircleShape)
                                NtStepState.NOW -> Modifier.border(2.dp, Nt.RedText, CircleShape)
                                NtStepState.TODO -> Modifier.border(2.dp, Nt.Outline, CircleShape)
                            }
                        )
                )
                NtBody(step.label, color = if (step.state == NtStepState.TODO) Nt.Grey else Nt.White)
            }
        }
    }
}

/** Project progress: selectable segments, finished ones red, current one has a sweeping bar. */
@Composable
fun NtStepIndicator(
    current: Int,
    modifier: Modifier = Modifier,
    total: Int = 5,
    onSelectStep: ((Int) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "steps")
    val sweep by transition.animateFloat(
        -0.5f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "sweep"
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { k ->
            val stepName = when (k) {
                0 -> androidx.compose.ui.res.stringResource(com.example.R.string.nt_step_studio)
                1 -> androidx.compose.ui.res.stringResource(com.example.R.string.nt_step_lyrics)
                2 -> androidx.compose.ui.res.stringResource(com.example.R.string.nt_step_visuals)
                3 -> androidx.compose.ui.res.stringResource(com.example.R.string.nt_step_sync)
                else -> androidx.compose.ui.res.stringResource(com.example.R.string.nt_step_export)
            }
            val description = androidx.compose.ui.res.stringResource(
                com.example.R.string.nt_step_label, k + 1, total, stepName
            )
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = k == current,
                        enabled = onSelectStep != null,
                        role = Role.Tab,
                        onClick = { onSelectStep?.invoke(k) }
                    )
                    .semantics { contentDescription = description }
                    .testTag(ProjectStep.entries.getOrNull(k)?.testTag ?: "project_step_${k + 1}"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxWidth().height(4.dp).clipToBounds()) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(if (k < current) colors.primary else colors.surfaceVariant, cornerRadius = radius)
                    if (k == current) {
                        val x = size.width * (if (reduced) 0.25f else sweep)
                        drawRoundRect(colors.primary, Offset(x, 0f), Size(size.width * 0.5f, size.height), radius)
                    }
                }
            }
        }
    }
}

/** The word being sung: red pill with a slowly moving red/pink gradient. */
@Composable
fun NtWordChip(text: String, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "word")
    val shift by transition.animateFloat(
        0f, 600f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "shift"
    )
    val offset = if (reduced) 0f else shift
    val brush = Brush.horizontalGradient(
        listOf(Nt.Red, Nt.Pink, Nt.Red),
        startX = offset,
        endX = offset + 300f,
        tileMode = TileMode.Repeated
    )
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .padding(horizontal = 6.dp),
        color = Nt.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}
