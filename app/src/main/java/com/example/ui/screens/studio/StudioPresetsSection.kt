package com.example.ui.screens.studio

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Preset
import com.example.ui.components.GlyphCard
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.StudioUiState

@Composable
fun StudioPresetsSection(
    state: StudioUiState,
    presets: List<Preset>,
    onOpenSavePresetDialog: () -> Unit,
    onLoadPreset: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Studio Presets Library
        GlyphCard(headerTitle = "STUDIO PRESETS & TEMPLATES") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_save_config_template),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingGrey,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingBlack)
                        .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                        .clickable { onOpenSavePresetDialog() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("save_as_preset_button")
                ) {
                    Text(
                        text = stringResource(R.string.studio_save_as_preset),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.studio_no_presets),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        color = NothingGrey
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .clickable { onLoadPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = preset.name,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                                Text(
                                    text = stringResource(R.string.studio_preset_meta, preset.genre, preset.mood),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = NothingGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
