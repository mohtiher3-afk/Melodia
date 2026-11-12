package com.example.ui.components

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingElevatedSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.network.LyricSection
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedGlow
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import kotlin.random.Random

@Composable
fun GlyphCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NothingCardSurface,
    borderColor: Color = NothingBorder,
    cornerRadius: Dp = 22.dp,
    hasRedDot: Boolean = false,
    headerTitle: String? = null,
    testTag: String = "glyph_card",
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .testTag(testTag)
            .shadow(4.dp, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clip(shape),
        shape = shape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(NothingElevatedSurface.copy(alpha = 0.32f), backgroundColor)))
                .padding(18.dp)
        ) {
            if ((headerTitle != null) || hasRedDot) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (headerTitle != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = headerTitle.uppercase(),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                color = NothingOffWhite
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (hasRedDot) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NothingRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.nothing_rec_on),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 9.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun NothingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isRedAccent: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "nothing_pill_button"
) {
    val bgColor = when {
        !enabled -> NothingDarkGrey
        isRedAccent -> NothingRed
        isPrimary -> NothingRed
        else -> NothingElevatedSurface
    }
    val textColor = when {
        !enabled -> NothingGrey
        isRedAccent -> NothingWhite
        isPrimary -> NothingWhite
        else -> NothingWhite
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .pressable(
                enabled = enabled,
                pressedScale = 0.96f,
                haptic = if (isPrimary || isRedAccent) CommitHaptic else TapHaptic,
                onClick = onClick
            )
            .border(
                width = if (isPrimary || isRedAccent) 0.dp else 1.dp,
                color = if (isPrimary || isRedAccent) Color.Transparent else NothingBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NothingSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "segmented_tabs"
) {
    Row(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            val interactionSource = remember { MutableInteractionSource() }
            val pressScale = rememberPressScale(interactionSource)
            val haptics = LocalHapticFeedback.current
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) NothingWhite else Color.Transparent,
                animationSpec = tween(180),
                label = "segment_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) NothingBlack else NothingGrey,
                animationSpec = tween(180),
                label = "segment_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .scale(pressScale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) {
                        haptics.performHapticFeedback(TapHaptic)
                        onSelectIndex(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun DotMatrixAudioMeter(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    barCount: Int = 18
) {
    val animState = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animState.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            animState.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val totalWidth = size.width
        val barWidth = (totalWidth / barCount) * 0.7f
        val gap = (totalWidth / barCount) * 0.3f

        for (i in 0 until barCount) {
            val randomFactor = if (isPlaying) {
                val seed = (i * 37 + (animState.value * 100).toInt()) % 100
                0.2f + (seed / 100f) * 0.8f
            } else {
                0.15f
            }
            val barHeight = size.height * randomFactor
            val x = i * (barWidth + gap)
            val y = size.height - barHeight

            val color = if (i >= barCount - 3) NothingRed else NothingWhite

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun NothingCoverArt(
    title: String,
    genre: String,
    modifier: Modifier = Modifier,
    imagePath: String? = null,
    testTag: String = "nothing_cover_art"
) {
    val hasValidImage = remember(imagePath) {
        !imagePath.isNullOrBlank() && File(imagePath).exists()
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(NothingBlack),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidImage) {
            AsyncImage(
                model = File(imagePath!!),
                contentDescription = "Cover Art: $title",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x22000000))
            )
        } else {
            Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Background geometric lines
            drawLine(
                color = NothingBorder,
                start = Offset(0f, h * 0.35f),
                end = Offset(w, h * 0.35f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = NothingBorder,
                start = Offset(w * 0.65f, 0f),
                end = Offset(w * 0.65f, h),
                strokeWidth = 1.dp.toPx()
            )

            // Center concentric glyph rings
            drawCircle(
                color = NothingBorder,
                radius = w * 0.28f,
                center = Offset(w * 0.45f, h * 0.5f),
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = NothingRed,
                radius = 8.dp.toPx(),
                center = Offset(w * 0.45f, h * 0.5f)
            )

            // Decorative matrix dots
            val dotRadius = 2.dp.toPx()
            val step = 16.dp.toPx()
            var x = 20.dp.toPx()
            while (x < w - 20.dp.toPx()) {
                var y = 20.dp.toPx()
                while (y < h * 0.3f) {
                    drawCircle(
                        color = Color(0x33FFFFFF),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }
        }

        // Overlay text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = genre.uppercase(),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                color = NothingRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontFamily = SpaceMonoFontFamily,
                fontSize = 16.sp,
                color = NothingWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // Top right track index
        Text(
            text = stringResource(R.string.nothing_brand_badge),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 9.sp,
            color = NothingGrey,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
    }
}

@Composable
fun NothingDotMatrixLoader(
    modifier: Modifier = Modifier,
    text: String = "PROCESSING // GEMINI & TREBLO AI..."
) {
    val animState = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NothingCardSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val w = size.width
            val h = size.height
            val cols = 12
            val spacing = w / (cols + 1)
            val progress = animState.value

            for (i in 0 until cols) {
                val waveOffset = kotlin.math.sin((progress * 2 * Math.PI) + (i * 0.5)).toFloat()
                val radius = 3.dp.toPx() + (waveOffset * 1.5f).coerceAtLeast(0f)
                val color = if (i == (progress * cols).toInt() % cols) NothingRed else NothingWhite
                val alpha = (0.3f + 0.7f * ((waveOffset + 1f) / 2f)).coerceIn(0.2f, 1f)

                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(spacing * (i + 1), h * 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text.uppercase(),
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 12.sp,
            color = NothingRed,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LyricTimelineEditor(
    sections: List<LyricSection>,
    onUpdateSection: (LyricSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NothingCardSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.nothing_timing_header),
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 12.sp,
            color = NothingRed,
            fontWeight = FontWeight.Bold
        )
        sections.forEach { section ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.nothing_section_tag, section.name.uppercase()),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = NothingWhite,
                    modifier = Modifier.width(80.dp)
                )

                // Simple Text input for start time in seconds
                androidx.compose.material3.OutlinedTextField(
                    value = (section.startTimeMs / 1000).toString(),
                    onValueChange = {
                        val newTime = it.toLongOrNull() ?: 0
                        onUpdateSection(section.copy(startTimeMs = newTime * 1000))
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start (s)", fontSize = 10.sp) },
                    singleLine = true
                )
            }
        }
    }
}


@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    // Strict monochrome status language: white = ready, grey = in progress,
    // red = the only failure/alert accent allowed by the design system.
    val color = when (status.uppercase()) {
        "READY", "DONE", "COMPLETED" -> NothingWhite
        "PROCESSING", "PENDING", "IDLE" -> NothingGrey
        "FAILED", "ERROR" -> NothingRed
        else -> NothingGrey
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.uppercase(),
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
