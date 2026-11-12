package com.example.ui.dialogs

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RecentSubGenre
import com.example.ui.components.GenreSelection
import com.example.ui.components.GenreSelection
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

@Composable
fun GenreSelectionDialog(
    selectedGenre: String,
    recentSubGenres: List<RecentSubGenre>,
    onGenreSelected: (name: String, category: String, tempoRange: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("genre_selection_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Text(
                                text = stringResource(R.string.genre_dialog_kicker),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 9.sp,
                                color = NothingRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.genre_dialog_header),
                            fontFamily = NothingDotMatrixFontFamily,
                            fontSize = 17.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NothingCardSurface)
                            .testTag("close_genre_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NothingWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Main GenreSelection content with Recently Viewed section at the top
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    GenreSelection(
                        selectedGenre = selectedGenre,
                        onGenreSelected = { name ->
                            onGenreSelected(name, "", "")
                            onDismiss()
                        },
                        recentSubGenres = recentSubGenres,
                        onGenreSelectedWithDetails = { name, category, tempo ->
                            onGenreSelected(name, category, tempo)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NothingPillButton(
                    text = stringResource(R.string.genre_dialog_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false,
                    testTag = "dismiss_genre_dialog_button"
                )
            }
        }
    }
}
