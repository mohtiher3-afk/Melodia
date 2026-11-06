package com.example.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.pressable
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.StudioUiState

@Composable
fun StudioHeader(
    state: StudioUiState,
    draftsCount: Int,
    onImportAudio: () -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Title & Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "MELODIA STUDIO // 01",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = NothingRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TEXT-FIRST LYRIC STUDIO",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingCardSurface)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (state.isLocalAudioActive) "AUDIO ON" else "TEXT ONLY",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 9.sp,
                    color = if (state.isLocalAudioActive) NothingRed else NothingWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Status & Local Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NothingCardSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("autosave_status_bar"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.isAutoSaving) NothingRed else NothingWhite)
                )
                Text(
                    text = when {
                        state.isAutoSaving -> "AUTO-SAVING DRAFT..."
                        state.lastAutoSavedTime != null -> "SAVED AT ${state.lastAutoSavedTime}"
                        else -> "AUTO-SAVE READY"
                    },
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingRed)
                        .pressable(
                            pressedScale = 0.93f,
                            haptic = com.example.ui.components.CommitHaptic
                        ) { onImportAudio() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ IMPORT AUDIO",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                        .pressable(
                            pressedScale = 0.93f,
                            haptic = com.example.ui.components.TapHaptic
                        ) { onOpenQueue() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("studio_open_queue")
                ) {
                    Text(
                        text = "QUEUE",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingGrey
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                        .pressable(
                            pressedScale = 0.93f,
                            haptic = com.example.ui.components.TapHaptic
                        ) { onOpenDrafts() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DRAFTS ($draftsCount)",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingGrey
                    )
                }
            }
        }
    }
}
