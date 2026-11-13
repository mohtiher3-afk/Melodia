package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
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
                NothingButton(
                    text = label,
                    onClick = { subTab = key },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_subtab_${key.lowercase()}"),
                    style = if (isSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
                    enabled = !isSelected
                )
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
