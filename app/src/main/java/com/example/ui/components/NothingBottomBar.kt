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
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
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
import com.example.ui.theme.NothingOutline
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.theme.NothingMutedGrey
import com.example.R
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

enum class MelodiaTab(val titleRes: Int, val icon: ImageVector, val tag: String) {
    STUDIO(R.string.tab_studio, Icons.Default.GraphicEq, "tab_studio"),
    LYRICS(R.string.tab_lyrics, Icons.Default.EditNote, "tab_lyrics"),
    LIBRARY(R.string.tab_library, Icons.Default.LibraryMusic, "tab_library"),
    SYNC(R.string.tab_sync, Icons.Default.Sync, "tab_sync"),
    MORE(R.string.tab_more, Icons.Default.MoreHoriz, "tab_more"),
    VISUALS(R.string.tab_visuals, Icons.Default.Image, "tab_visuals"),
    EXPORT(R.string.tab_export, Icons.Default.Upload, "tab_export"),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings, "tab_settings")
}

private val primaryTabs = listOf(
    MelodiaTab.STUDIO,
    MelodiaTab.LYRICS,
    MelodiaTab.LIBRARY,
    MelodiaTab.SYNC,
    MelodiaTab.MORE
)

private val secondaryTabs = setOf(MelodiaTab.VISUALS, MelodiaTab.EXPORT, MelodiaTab.SETTINGS)

/**
 * Equal-width tab bar for five primary destinations. Secondary destinations live under More.
 * Only the selected tab shows its name, which lets
 * the icons be larger and the label readable (10 sp instead of 8 sp). Every item reserves the
 * same label slot, so nothing shifts when the selection changes. Unselected labels stay in the
 * semantics tree (alpha 0), so screen readers and UI tests still find every tab by name.
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
            primaryTabs.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = currentTab == tab || (tab == MelodiaTab.MORE && currentTab in secondaryTabs),
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
    // Fixed-height slot, only its width animates: the red marker grows under the selected tab.
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

        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(tab.titleRes),
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
