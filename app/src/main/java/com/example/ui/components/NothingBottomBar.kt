package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingElevatedSurface
import com.example.ui.theme.NothingMutedGrey
import com.example.ui.theme.NothingOutline
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

enum class MelodiaTab(
    val title: String,
    val dotIcon: DotIcon,
    val icon: ImageVector,
    val tag: String
) {
    STUDIO("Studio", DotIcon.STUDIO, Icons.Default.GraphicEq, "tab_studio"),
    LYRICS("Lyrics", DotIcon.LYRICS, Icons.Default.EditNote, "tab_lyrics"),
    LIBRARY("Library", DotIcon.LIBRARY, Icons.Default.LibraryMusic, "tab_library"),
    VISUALS("Visuals", DotIcon.VISUALS, Icons.Default.Image, "tab_visuals"),
    SYNC("Sync", DotIcon.SYNC, Icons.Default.Sync, "tab_sync"),
    EXPORT("Export", DotIcon.EXPORT, Icons.Default.Upload, "tab_export"),
    SETTINGS("Settings", DotIcon.SETTINGS, Icons.Default.Settings, "tab_settings")
}

/**
 * Equal-width tab bar for seven destinations using Nothing DotMatrixIcons.
 */
@Composable
fun NothingBottomBar(
    currentTab: MelodiaTab,
    onTabSelected: (MelodiaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nothing_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(NothingCardSurface)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(NothingOutline, NothingBorder)),
                    RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MelodiaTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: MelodiaTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) NothingRed else NothingMutedGrey,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_item_color"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) NothingRed.copy(alpha = 0.10f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "bottom_bar_item_container"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "bottom_bar_label_alpha"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_bar_indicator_width"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.9f)
    val haptics = LocalHapticFeedback.current
    val itemShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .scale(pressScale)
            .clip(itemShape)
            .background(containerColor)
            .selectable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                selected = selected,
                onClick = {
                    haptics.performHapticFeedback(TapHaptic)
                    onClick()
                },
                role = Role.Tab
            )
            .testTag(tab.tag)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NothingRed)
        )

        Spacer(modifier = Modifier.height(6.dp))

        DotMatrixIcon(
            icon = tab.dotIcon,
            contentDescription = null,
            size = 24.dp,
            tint = contentColor,
            animated = selected
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.title,
            fontFamily = SpaceMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .alpha(labelAlpha)
        )
    }
}