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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingMutedGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

enum class MelodiaTab(val title: String, val icon: ImageVector, val tag: String) {
    STUDIO("STUDIO", Icons.Default.GraphicEq, "tab_studio"),
    LYRICS("LYRICS", Icons.Default.EditNote, "tab_lyrics"),
    LIBRARY("LIBRARY", Icons.Default.LibraryMusic, "tab_library"),
    VISUALS("VISUALS", Icons.Default.Image, "tab_visuals"),
    SYNC("SYNC", Icons.Default.Sync, "tab_sync"),
    EXPORT("EXPORT", Icons.Default.Upload, "tab_export"),
    SETTINGS("CONFIG", Icons.Default.Settings, "tab_settings")
}

/**
 * Equal-width tab bar. Every destination keeps its icon AND its label visible
 * so navigation is discoverable at a glance, and the selected tab never
 * changes the layout of its neighbours (no more shifting tap targets).
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
                .height(68.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NothingCardSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MelodiaTab.values().forEach { tab ->
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
        targetValue = if (selected) NothingWhite else NothingMutedGrey,
        animationSpec = tween(durationMillis = 180),
        label = "bottom_bar_item_color"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource, pressedScale = 0.9f)
    val haptics = LocalHapticFeedback.current

    // Soft shadow elevation when tab is selected (using background alpha animation instead of elevation API)
    val shadowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.15f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "bottom_bar_shadow"
    )

    // The indicator slot keeps its height while the bar itself grows, so the
    // icon and label never move when the selection changes.
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 18.dp else 4.dp,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_bar_indicator_width"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) NothingRed else NothingBorder,
        animationSpec = tween(durationMillis = 220),
        label = "bottom_bar_indicator_color"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) NothingCardSurface.copy(alpha = 0.6f) else NothingCardSurface
            )
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
        // Soft shadow under the selected tab (via background alpha animation)
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(indicatorColor.copy(alpha = shadowAlpha))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = tab.title,
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 8.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.sp,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
