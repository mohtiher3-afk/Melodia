package com.example.ui.dialogs

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import com.example.ui.components.pressable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.LyricDraft
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DraftsDialog(
    drafts: List<LyricDraft>,
    activeDraftId: Long?,
    onRestoreDraft: (LyricDraft) -> Unit,
    onDeleteDraft: (LyricDraft) -> Unit,
    onClearAllDrafts: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd / HH:mm:ss", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("drafts_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                                text = stringResource(R.string.drafts_db_kicker),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.drafts_saved_count, drafts.size),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NothingCardSurface)
                            .testTag("close_drafts_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.drafts_autosave_help),
                    fontSize = 11.sp,
                    color = NothingGrey,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Drafts list
                if (drafts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NothingCardSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.drafts_empty),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.drafts_empty_help),
                                fontSize = 11.sp,
                                color = NothingGrey,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drafts_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(drafts, key = { it.id }) { draft ->
                            val isCurrent = draft.id == activeDraftId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isCurrent) NothingDarkGrey else NothingCardSurface)
                                    .border(
                                        1.dp,
                                        if (isCurrent) NothingRed else NothingBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .pressable(
                    pressedScale = 0.94f,
                    haptic = com.example.ui.components.TapHaptic,
                    onClick = {
                        onRestoreDraft(draft)
                        onDismiss()
                    }
                )
                                    .padding(14.dp)
                                    .testTag("draft_item_${draft.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NothingRed)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.drafts_active_badge),
                                                    fontFamily = SpaceMonoFontFamily,
                                                    fontSize = 8.sp,
                                                    color = NothingWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = draft.title.ifBlank { "Untitled Draft" },
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteDraft(draft) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_draft_${draft.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Draft",
                                            tint = NothingGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.drafts_meta_line, draft.genre.uppercase(), draft.mood.uppercase()),
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingRed
                                    )
                                    Text(
                                        text = stringResource(R.string.drafts_meta_slash),
                                        color = NothingGrey,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = dateFormat.format(Date(draft.lastModified)),
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingGrey
                                    )
                                }

                                if (draft.userTopic.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.drafts_topic_label, draft.userTopic),
                                        fontSize = 11.sp,
                                        color = NothingOffWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (draft.lyrics.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val lyricSnippet = draft.lyrics.lines().filter { it.isNotBlank() }.take(2).joinToString(" / ")
                                    Text(
                                        text = stringResource(R.string.drafts_snippet_quote, lyricSnippet),
                                        fontSize = 11.sp,
                                        color = NothingGrey,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NothingBlack)
                                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                            .pressable(
                    pressedScale = 0.94f,
                    haptic = com.example.ui.components.TapHaptic,
                    onClick = {
                        onRestoreDraft(draft)
                        onDismiss()
                    }
                )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Restore,
                                                contentDescription = null,
                                                tint = NothingWhite,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.drafts_restore),
                                                fontFamily = SpaceMonoFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (drafts.isNotEmpty()) {
                        NothingPillButton(
                            text = stringResource(R.string.drafts_clear_all),
                            onClick = onClearAllDrafts,
                            modifier = Modifier.weight(1f),
                            isPrimary = false,
                            testTag = "clear_all_drafts_button"
                        )
                    }

                    NothingPillButton(
                        text = stringResource(R.string.drafts_close),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        isPrimary = drafts.isEmpty(),
                        testTag = "close_drafts_button"
                    )
                }
            }
        }
    }
}
