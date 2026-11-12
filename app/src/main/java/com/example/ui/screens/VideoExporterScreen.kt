package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import com.example.domain.VideoAspectRatio
import com.example.domain.VideoResolution
import com.example.ui.components.GlyphCard
import java.io.File
import com.example.ui.components.NothingPillButton
import com.example.ui.components.StatusBadge
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
fun VideoExporterScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentDraft = state.currentDraft

    var selectedResolution by remember { mutableStateOf(VideoResolution.RES_1080P) }
    var selectedAspectRatio by remember { mutableStateOf(VideoAspectRatio.PORTRAIT_9_16) }
    var bitrateMbps by remember { mutableStateOf(12) }

    val exportProgress = state.exportProgress

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("video_exporter_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = stringResource(R.string.exporter_header_kicker),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                color = NothingRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = stringResource(R.string.exporter_title),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )
        }

        if (currentDraft == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .background(NothingCardSurface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.exporter_no_draft),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
            }
            return
        }

        // Readiness Check Card
        GlyphCard(headerTitle = "EXPORT READINESS CHECKLIST") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasLyrics = currentDraft.lyrics.isNotBlank()
                val hasCover = !currentDraft.coverImagePath.isNullOrBlank()
                val hasAudio = state.isLocalAudioActive
                val hasSync = state.karaokeLyrics != null

                ChecklistItem("Structured Lyrics Drafted", hasLyrics)
                ChecklistItem("Cover Art Generated", hasCover)
                ChecklistItem("Local Audio Track Imported", hasAudio, isOptional = true)
                ChecklistItem("Whisper Timestamps Aligned", hasSync, isOptional = true)
            }
        }

        // Resolution Selector
        GlyphCard(headerTitle = "VIDEO RESOLUTION & PROFILE") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoResolution.values().forEach { res ->
                        val isSel = selectedResolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NothingRed else NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedResolution = res },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res.label,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) NothingBlack else NothingWhite
                            )
                        }
                    }
                }

                // Bitrate selector (F-6: was hardcoded at 12 Mbps with no UI)
                Text(
                    text = stringResource(R.string.exporter_bitrate_label),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingGrey
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(8, 12, 20, 35).forEach { mbps ->
                        val isSel = bitrateMbps == mbps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NothingRed else NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .clickable { bitrateMbps = mbps },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.exporter_bitrate_mbps, mbps),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) NothingBlack else NothingWhite
                            )
                        }
                    }
                }
            }
        }

        // Aspect Ratio Selector
        GlyphCard(headerTitle = "ASPECT RATIO") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VideoAspectRatio.values().forEach { ratio ->
                    val isSel = selectedAspectRatio == ratio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) NothingCardSurface else NothingBlack)
                            .border(1.dp, if (isSel) NothingRed else NothingBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedAspectRatio = ratio }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ratio.label,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = NothingWhite
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSel) NothingRed else NothingDarkGrey)
                        )
                    }
                }
            }
        }

        // Export Progress
        if (exportProgress.isExporting) {
            GlyphCard(headerTitle = "ENCODING IN PROGRESS") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = exportProgress.statusText,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingRed
                    )
                    LinearProgressIndicator(
                        progress = { exportProgress.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = NothingRed,
                        trackColor = NothingBorder
                    )
                }
            }
        }

        // Export complete state (was invisible: the progress card vanished on
        // completion and the Share button appearing was the only cue)
        val outputFilePath = exportProgress.outputFilePath
        val exportFinished = !exportProgress.isExporting &&
            outputFilePath != null && File(outputFilePath).exists()
        if (exportFinished) {
            GlyphCard(headerTitle = stringResource(R.string.exporter_complete_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = exportProgress.statusText.ifBlank { stringResource(R.string.exporter_complete_default) },
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        color = NothingRed
                    )
                    Text(
                        text = File(outputFilePath).name,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
                    )
                }
            }
        }

        // Export Action Button (guarded: one encode at a time — double MediaCodec
        // encodes would race on the muxer and corrupt output)
        NothingPillButton(
            text = if (exportProgress.isExporting) "ENCODING VIDEO..." else "EXPORT MP4 LYRIC VIDEO",
            onClick = {
                viewModel.exportLyricVideo(
                    context = context,
                    resolution = selectedResolution,
                    aspectRatio = selectedAspectRatio,
                    bitrateMbps = bitrateMbps
                )
            },
            isPrimary = true,
            enabled = !exportProgress.isExporting,
            modifier = Modifier.fillMaxWidth()
        )

        // Share the finished export (F-6: result was previously unreachable outside app cache)
        if (exportFinished) {
            NothingPillButton(
                text = stringResource(R.string.exporter_share_video),
                onClick = {
                    try {
                        val videoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            File(outputFilePath)
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(android.content.Intent.EXTRA_STREAM, videoUri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(shareIntent, "Share lyric video")
                        )
                    } catch (e: Exception) {
                        Log.e("VideoExporterScreen", "Failed to share video", e)
                    }
                },
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChecklistItem(label: String, isComplete: Boolean, isOptional: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label + if (isOptional) " (Optional)" else "",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 12.sp,
                color = if (isComplete) NothingWhite else NothingGrey
            )
        }
        StatusBadge(
            status = if (isComplete) "READY" else "NOT READY"
        )
    }
}

