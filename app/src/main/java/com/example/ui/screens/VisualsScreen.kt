package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlyphCard
import com.example.ui.components.NothingCoverArt
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun VisualsScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val draft = state.currentDraft

        GlyphCard(headerTitle = "COVER ART GENERATOR") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header with StatusBadge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISUAL PREVIEW",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )
                    StatusBadge(
                        status = when {
                            state.isGeneratingCover -> "PROCESSING"
                            draft?.coverImagePath != null -> "READY"
                            else -> "IDLE"
                        }
                    )
                }

                NothingCoverArt(
                    title = draft?.title ?: "Melodia",
                    genre = draft?.genre ?: "Pop",
                    imagePath = draft?.coverImagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NothingPillButton(
                        text = if (state.isGeneratingCover) "GENERATING..." else "GENERATE COVER ART",
                        onClick = { viewModel.generateCoverArtOnly() },
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

