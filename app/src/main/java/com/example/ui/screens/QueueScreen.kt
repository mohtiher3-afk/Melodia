package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.components.pressable
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun QueueScreen(
    viewModel: MelodiaViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val drafts by viewModel.savedDrafts.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(16.dp)
            .testTag("queue_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(NothingRed)
                    )
                    Text(
                        text = stringResource(R.string.queue_header_kicker),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.queue_active_projects),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 22.sp,
                    color = NothingWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onClose != null) {
                NothingButton(
                    text = "",
                    onClick = onClose,
                    style = NothingButtonStyle.IconOnly,
                    icon = Icons.Default.Close,
                    modifier = Modifier.testTag("queue_close")
                )
            }
        }

        if (drafts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .background(NothingCardSurface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Queue Empty",
                        tint = NothingGrey,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = stringResource(R.string.queue_empty),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 14.sp,
                        color = NothingWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.queue_empty_help),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingDarkGrey
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NothingButton(
                        text = stringResource(R.string.queue_create_lyrics),
                        onClick = onNavigateToStudio,
                        style = NothingButtonStyle.Primary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(drafts, key = { it.id }) { draft ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingCardSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = draft.title.ifBlank { "Untitled Draft" },
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Saved",
                                    tint = NothingRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.queue_draft_meta, draft.genre, draft.mood, draft.language),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 11.sp,
                                color = NothingGrey
                            )
                            NothingButton(
                                text = stringResource(R.string.queue_load_in_studio),
                                onClick = {
                                    viewModel.restoreDraft(draft)
                                    onNavigateToStudio()
                                },
                                style = NothingButtonStyle.Secondary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}
