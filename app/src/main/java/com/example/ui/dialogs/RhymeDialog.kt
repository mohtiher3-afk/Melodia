package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.RhymeAnalyzer
import com.example.domain.RhymeResult
import com.example.ui.components.CommitHaptic
import com.example.ui.components.NothingPillButton
import com.example.ui.components.pressable
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

/**
 * Rhyme and synonym helper for the lyrics editor. Tapping a suggestion inserts
 * it into the draft, so the dialog is a writing tool rather than a dead-end
 * dictionary.
 */
@Composable
fun RhymeDialog(
    query: String,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<RhymeResult?>(null) }
    var lastInserted by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        isLoading = true
        result = runCatching { RhymeAnalyzer.findRhymes(query) }.getOrNull()
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(NothingDarkSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("rhyme_dialog")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        text = "LYRICS // RHYME ENGINE",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        color = NothingRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "RHYMES FOR \"${query.trim().uppercase()}\"",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }

                when {
                    isLoading -> Text(
                        text = "ANALYZING SOUND PATTERNS...",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )

                    result == null -> Text(
                        text = "RHYME ENGINE UNAVAILABLE. CHECK YOUR CONNECTION.",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingGrey
                    )

                    else -> {
                        val rhymes = result!!
                        SuggestionRow(
                            title = "EXACT RHYMES",
                            values = rhymes.exactRhymes,
                            onPick = { onInsert(it); lastInserted = it }
                        )
                        SuggestionRow(
                            title = "SLANT RHYMES",
                            values = rhymes.slantRhymes,
                            onPick = { onInsert(it); lastInserted = it }
                        )
                        SuggestionRow(
                            title = "POETIC SYNONYMS",
                            values = rhymes.synonyms,
                            onPick = { onInsert(it); lastInserted = it }
                        )

                        Text(
                            text = lastInserted?.let { "INSERTED: $it" } ?: "TAP A WORD TO INSERT IT INTO THE DRAFT",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = if (lastInserted != null) NothingRed else NothingGrey,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NothingPillButton(
                        text = "DONE",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    title: String,
    values: List<String>,
    onPick: (String) -> Unit
) {
    if (values.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontFamily = SpaceMonoFontFamily,
            fontSize = 10.sp,
            color = NothingGrey,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            values.forEach { value ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingDarkSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .pressable(
                            pressedScale = 0.92f,
                            haptic = CommitHaptic
                        ) { onPick(value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("rhyme_chip_$value")
                ) {
                    Text(
                        text = value,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingWhite
                    )
                }
            }
        }
    }
}
