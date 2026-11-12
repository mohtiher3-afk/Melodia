package com.example.ui.screens.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlyphCard
import com.example.ui.components.GenerationActionFeedback
import com.example.ui.components.NothingPillButton
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.viewmodel.StudioUiState

@Composable
fun StudioTopicSection(
    state: StudioUiState,
    onSetUserTopic: (String) -> Unit,
    onGenerateLyrics: () -> Unit,
    onGeneratePrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Song Topic & Inspiration Input
        GlyphCard(headerTitle = "TOPIC & SONG INSPIRATION") {
            OutlinedTextField(
                value = state.userTopic,
                onValueChange = { onSetUserTopic(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("topic_input"),
                placeholder = {
                    Text(
                        "e.g. Midnight city lights, late night ambition, old memories...",
                        color = NothingGrey,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingRed,
                    unfocusedBorderColor = NothingBorder,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite,
                    cursorColor = NothingRed
                ),
                shape = RoundedCornerShape(14.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NothingPillButton(
                    text = if (state.isGenerating) "DRAFTING LYRICS..." else "CREATE LYRICS",
                    onClick = onGenerateLyrics,
                    isPrimary = true,
                    enabled = !state.isGenerating && !state.isPolishingPrompt,
                    modifier = Modifier.weight(1f)
                )

                NothingPillButton(
                    text = if (state.isPolishingPrompt) "POLISHING..." else "GENERATE PROMPT",
                    onClick = onGeneratePrompt,
                    enabled = state.currentDraft != null && !state.isGenerating && !state.isPolishingPrompt,
                    modifier = Modifier.weight(1f)
                )
            }

            when {
                state.isGenerating -> GenerationActionFeedback(
                    message = state.lyricsFeedback?.message ?: "Creating lyrics...",
                    isLoading = true
                )
                state.isPolishingPrompt -> GenerationActionFeedback(
                    message = state.promptFeedback?.message ?: "Creating the text prompt...",
                    isLoading = true
                )
                state.currentDraft == null -> GenerationActionFeedback(
                    message = "Create or restore a draft first to generate a prompt."
                )
                state.lyricsFeedback != null -> GenerationActionFeedback(
                    message = state.lyricsFeedback.message,
                    isError = state.lyricsFeedback.isError
                )
                state.promptFeedback != null -> GenerationActionFeedback(
                    message = state.promptFeedback.message,
                    isError = state.promptFeedback.isError
                )
            }
        }
    }
}
