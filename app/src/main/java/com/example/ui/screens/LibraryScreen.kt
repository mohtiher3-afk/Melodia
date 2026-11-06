package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.example.ui.components.pressable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun LibraryScreen(
    viewModel: MelodiaViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf("TRACKS") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .testTag("library_screen")
    ) {
        // Sub-navigation top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(NothingCardSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(22.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "TRACKS & LYRICS" to "TRACKS",
                "VIDEO EXPORT" to "VIDEO"
            )
            tabs.forEach { (label, key) ->
                val isSelected = subTab == key
                val tabBackground by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) NothingRed else NothingCardSurface,
                    animationSpec = androidx.compose.animation.core.tween(180),
                    label = "library_subtab_bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(tabBackground)
                        .pressable(
                            pressedScale = 0.95f,
                            haptic = com.example.ui.components.TapHaptic
                        ) { subTab = key }
                        .testTag("library_subtab_${key.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NothingWhite else NothingGrey
                    )
                }
            }
        }

        // Screen content
        Box(modifier = Modifier.fillMaxSize()) {
            when (subTab) {
                "TRACKS" -> VaultScreen(viewModel = viewModel, onNavigateToStudio = onNavigateToStudio)
                "VIDEO" -> VideoExporterScreen(viewModel = viewModel)
            }
        }
    }
}
