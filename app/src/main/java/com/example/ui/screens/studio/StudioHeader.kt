package com.example.ui.screens.studio

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MelodiaActionButton
import com.example.ui.MelodiaButtonStyle
import com.example.ui.MelodiaRadius
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingMutedGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.StudioUiState

/**
 * Top of the Studio tab: the app name set large in the dot-matrix face (the one memorable
 * element on the screen), the current mode, a quiet save indicator, and the three actions
 * sized as proper 48 dp touch targets.
 */
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = stringResource(R.string.studio_app_name),
                    fontFamily = NothingDotMatrixFontFamily,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    color = NothingWhite,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.studio_app_tagline),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            ModeChip(isAudioActive = state.isLocalAudioActive, modifier = Modifier.padding(top = 8.dp))
        }

        SaveStatusLine(
            isSaving = state.isAutoSaving,
            lastSavedTime = state.lastAutoSavedTime,
            modifier = Modifier.testTag("autosave_status_bar")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MelodiaActionButton(
                text = stringResource(R.string.studio_import_audio),
                onClick = onImportAudio,
                icon = Icons.Default.Upload,
                style = MelodiaButtonStyle.Primary,
                modifier = Modifier.weight(1.35f)
            )
            MelodiaActionButton(
                text = stringResource(R.string.studio_queue),
                onClick = onOpenQueue,
                modifier = Modifier
                    .weight(1f)
                    .testTag("studio_open_queue")
            )
            MelodiaActionButton(
                text = stringResource(R.string.studio_drafts_count, draftsCount),
                onClick = onOpenDrafts,
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@Composable
private fun ModeChip(isAudioActive: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(MelodiaRadius.chip)
    Row(
        modifier = modifier
            .clip(shape)
            .background(NothingCardSurface)
            .border(1.dp, NothingBorder, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isAudioActive) NothingRed else NothingMutedGrey)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isAudioActive) stringResource(R.string.studio_header_audio_on) else stringResource(R.string.studio_header_text_only),
            fontFamily = SpaceMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAudioActive) NothingWhite else NothingGrey,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Single quiet line instead of a boxed bar; the dot pulses only while a save is running. */
@Composable
private fun SaveStatusLine(
    isSaving: Boolean,
    lastSavedTime: String?,
    modifier: Modifier = Modifier
) {
    val dotAlpha = if (isSaving) {
        val transition = rememberInfiniteTransition(label = "save_dot")
        val pulse by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "save_dot_alpha"
        )
        pulse
    } else {
        1f
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(7.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(if (isSaving) NothingRed else NothingGrey)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when {
                isSaving -> stringResource(R.string.studio_autosave_saving)
                lastSavedTime != null -> stringResource(R.string.studio_autosave_saved_at, lastSavedTime)
                else -> stringResource(R.string.studio_autosave_on)
            },
            fontFamily = SpaceMonoFontFamily,
            fontSize = 11.sp,
            color = NothingGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
