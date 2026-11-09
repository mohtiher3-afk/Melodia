package com.example.ui.nothing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.dotGrid
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.SpaceMonoFontFamily

data class NtNavItem(val label: String, val icon: DotIcon, val testTag: String)

/** The three bottom destinations: Project, Library, Settings. */
@Composable
fun rememberNtNavItems(): List<NtNavItem> {
    val project = stringResource(R.string.nt_nav_project)
    val library = stringResource(R.string.nt_nav_library)
    val settings = stringResource(R.string.nt_nav_settings)
    return remember(project, library, settings) {
        listOf(
            NtNavItem(project, DotIcon.STUDIO, NtDestination.PROJECT.testTag),
            NtNavItem(library, DotIcon.LIBRARY, NtDestination.LIBRARY.testTag),
            NtNavItem(settings, DotIcon.SETTINGS, NtDestination.SETTINGS.testTag)
        )
    }
}

/** "STEP 2 / 5 . LYRICS" for the project flow (step is zero-based). */
@Composable
fun ntStepLabel(step: Int): String {
    val name = when (step) {
        0 -> stringResource(R.string.nt_step_studio)
        1 -> stringResource(R.string.nt_step_lyrics)
        2 -> stringResource(R.string.nt_step_visuals)
        3 -> stringResource(R.string.nt_step_sync)
        else -> stringResource(R.string.nt_step_export)
    }
    return stringResource(R.string.nt_step_label, step + 1, 5, name)
}

@Composable
fun NtTopBar(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = Nt.White,
            fontFamily = NothingDotMatrixFontFamily,
            fontSize = 22.sp,
            letterSpacing = 0.04.em
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            NtPulsingLed()
            Spacer(Modifier.width(8.dp))
            NtLabel(label)
        }
    }
}

@Composable
fun NtBottomNav(
    items: List<NtNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Nt.Black)
            .drawBehind {
                drawRect(Nt.Border, size = Size(size.width, 1.dp.toPx()))
            }
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .drawBehind {
                        if (selected) drawRect(Nt.Red, size = Size(size.width, 2.dp.toPx()))
                    }
                    .testTag(item.testTag)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DotMatrixIcon(
                    icon = item.icon,
                    contentDescription = null,
                    iconSize = 28.dp,
                    tint = if (selected) Nt.White else Nt.Grey,
                    animated = selected
                )
                Text(
                    text = item.label,
                    modifier = Modifier.padding(top = 6.dp),
                    color = if (selected) Nt.White else Nt.Grey,
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.08.em
                )
            }
        }
    }
}

/**
 * Screen frame: black dot-grid background, top bar, optional project step indicator,
 * scrolling content, optional [bottomExtra] (mini player) and the bottom navigation.
 */
@Composable
fun NtAppShell(
    selectedDestination: Int,
    onSelectDestination: (Int) -> Unit,
    topLabel: String,
    modifier: Modifier = Modifier,
    stepIndex: Int? = null,
    onSelectStep: ((Int) -> Unit)? = null,
    scrollContent: Boolean = true,
    bottomExtra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides Nt.White) {
        Column(
            modifier
                .fillMaxSize()
                .background(Nt.Black)
                .dotGrid(spacing = 20.dp)
                .statusBarsPadding()
        ) {
            if (scrollContent) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Nt.ScreenPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NtTopBar(topLabel)
                    if (stepIndex != null) {
                        NtStepIndicator(stepIndex, onSelectStep = onSelectStep)
                    }
                    content()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Nt.ScreenPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NtTopBar(topLabel)
                    if (stepIndex != null) {
                        NtStepIndicator(stepIndex, onSelectStep = onSelectStep)
                    }
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                }
            }
            bottomExtra?.invoke()
            NtBottomNav(
                items = rememberNtNavItems(),
                selectedIndex = selectedDestination,
                onSelect = onSelectDestination,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}
