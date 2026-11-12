package com.example.ui.screens.studio

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlyphCard
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.GenerationVersion
import com.example.ui.viewmodel.StudioUiState

@Composable
fun StudioVersionHistorySection(
    state: StudioUiState,
    onRevertToVersion: (GenerationVersion) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (state.versionHistory.isNotEmpty()) {
        GlyphCard(
            headerTitle = "VERSION HISTORY // LAST 5 CLIPS",
            hasRedDot = true,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.studio_revert_history),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 9.sp,
                color = NothingGrey,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.versionHistory.forEachIndexed { index, version ->
                    val isCurrent = state.currentDraft?.title == version.title
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) NothingDarkSurface else NothingBlack)
                            .border(1.dp, if (isCurrent) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .testTag("version_item_$index")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCurrent) NothingRed else NothingCardSurface)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.studio_version_label, state.versionHistory.size - index, version.timestamp),
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite
                                    )
                                }

                                Text(
                                    text = version.title,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite,
                                    maxLines = 1
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) NothingDarkSurface else NothingCardSurface)
                                    .border(1.dp, if (isCurrent) NothingGrey else NothingRed, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onRevertToVersion(version)
                                        Toast.makeText(context, "Reverted to ${version.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("revert_version_button_$index")
                            ) {
                                Text(
                                    text = if (isCurrent) "ACTIVE" else "REVERT PROMPT",
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) NothingGrey else NothingRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VersionTagChip(version.genre)
                            VersionTagChip(version.vocalStyle)
                            VersionTagChip(version.bpm)
                            VersionTagChip(version.language)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = version.promptText,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingGrey,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionTagChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontFamily = SpaceMonoFontFamily,
            fontSize = 9.sp,
            color = NothingGrey
        )
    }
}
