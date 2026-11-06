package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilledButton
import androidx.compose.material3.ProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.GlyphCard
import com.example.ui.components.TapHaptic
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
fun SettingsScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val defaultExportFormat by viewModel.defaultExportFormat.collectAsStateWithLifecycle()
    val llmProviderId by viewModel.llmProviderId.collectAsStateWithLifecycle()
    val allowCloudTranscription by viewModel.allowCloudTranscription.collectAsStateWithLifecycle()
    val isGeminiConfigured = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    val isFreeLlmConfigured = BuildConfig.FREELLM_BASE_URL.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "CONFIG // SYSTEM",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                color = NothingRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Melodia Studio Settings",
                fontFamily = SpaceMonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )
        }

        // Cloud key status (not required by the local provider)
        GlyphCard(headerTitle = "GEMINI CLOUD API") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Key Icon",
                        tint = NothingRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Cloud Gemini API Key",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = if (isGeminiConfigured) "Configured; cloud requests may use quota/billing" else "Not configured — not needed for local Nano",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = NothingGrey
                        )
                    }
                }
                Icon(
                    imageVector = if (isGeminiConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGeminiConfigured) "Active" else "Not configured",
                    tint = if (isGeminiConfigured) NothingRed else NothingGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // LLM Provider Selection Card
        GlyphCard(headerTitle = "LLM PROVIDER") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nano runs on-device on supported phones; Android AICore may download its model once. Cloud options may have quotas or charges:",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf<Pair<String, String>>(
                        com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO to "Nano Local",
                        com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI to "Gemini API",
                        com.example.network.llm.LlmProviderFactory.PROVIDER_OPENROUTER to "Other Cloud"
                    ).forEach { (id, label) ->
                        val isSel = llmProviderId == id
                        val configured = when (id) {
                            com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO -> true
                            com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI -> isGeminiConfigured
                            else -> isFreeLlmConfigured
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NothingRed else NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .pressable(
                                    pressedScale = 0.95f,
                                    haptic = com.example.ui.components.TapHaptic
                                ) { viewModel.updateLlmProvider(id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NothingBlack else NothingWhite
                                )
                                Text(
                                    text = when {
                                        id == com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO -> "DEVICE CHECK"
                                        configured -> "CONFIGURED"
                                        else -> "NOT CONFIGURED"
                                    },
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = if (isSel) NothingBlack else NothingGrey
                                )
                            }
                        }
                    }
                }
            }
        }

                // Local Model Downloads Card
                        GlyphCard(headerTitle = "LOCAL MODEL DOWNLOADS") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Download free, open-source AI models directly to this device. No API keys, no cloud. Each model downloads with one tap.",
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = NothingGrey
                                )
                                LocalModelDownloadSection(viewModel = viewModel)
                            }
                        }

                        // Local Audio Privacy Card
                        GlyphCard(headerTitle = "LOCAL AUDIO PRIVACY") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Allow Gemini cloud transcription",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = if (allowCloudTranscription) "Imported audio may be sent online (up to 14 MB)." else "Off by default. Local audio stays on this device; Whisper model integration is pending.",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
                    )
                }
                Switch(
                    checked = allowCloudTranscription,
                    onCheckedChange = viewModel::setAllowCloudTranscription
                )
            }
        }

        // Default Export Format Card
        GlyphCard(headerTitle = "DEFAULT TIMING EXPORT FORMAT") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select default timing format when exporting synced lyrics:",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LRC", "JSON", "TXT", "DOC").forEach { fmt ->
                        val isSel = defaultExportFormat == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NothingRed else NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .pressable(
                                    pressedScale = 0.95f,
                                    haptic = com.example.ui.components.TapHaptic
                                ) { viewModel.updateDefaultExportFormat(fmt) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) NothingBlack else NothingWhite
                            )
                        }
                    }
                }
            }
        }

        // About & Version Info
        GlyphCard(headerTitle = "MELODIA STUDIO ARCHITECTURE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Melodia v3.2 Text-First Lyric Studio",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
                Text(
                    text = "// Gemini Nano lyrics on supported phones\n// Cloud audio transcription is OFF by default\n// Local Whisper and image-diffusion models are pending",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = NothingGrey
                )
            }
        }
    }
}

// Local Model Download Section - shows available models with one-tap download buttons
@Composable
fun LocalModelDownloadSection(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val modelManager = remember { com.example.domain.LocalModelManager(context) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        com.example.domain.LocalModelManager.AVAILABLE_MODELS.forEach { model ->
            val downloadState by modelManager.downloadStates[model.id]?.collectAsStateWithLifecycle()
                ?: remember { mutableStateOf(com.example.domain.LocalModelManager.DownloadState.Idle(modelManager.isModelDownloaded(model))) }

            GlyphCard(headerTitle = model.name) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = model.description,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (downloadState) {
                        is com.example.domain.LocalModelManager.DownloadState.Idle -> {
                            if (downloadState.isModelPresent) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = NothingRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "READY",
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingRed
                                        )
                                        Text(
                                            text = "Remove",
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = NothingGrey,
                                            modifier = Modifier.clickable { viewModel.deleteModel(model.id) }
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Size: ${com.example.domain.LocalModelManager.formatBytes(model.expectedSizeBytes)}",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingGrey
                                    )
                                    androidx.compose.material3.FilledButton(
                                        onClick = { viewModel.downloadModel(model.id) },
                                        modifier = Modifier.testTag("download_${model.id}")
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Download",
                                                tint = NothingBlack,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "DOWNLOAD",
                                                fontFamily = SpaceMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is com.example.domain.LocalModelManager.DownloadState.Downloading -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.material3.ProgressIndicator(
                                    progress = downloadState.progress,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NothingRed,
                                    trackColor = NothingDarkGrey
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(downloadState.progress * 100).toInt()}% • ${com.example.domain.LocalModelManager.formatBytes(downloadState.downloadedBytes)} / ${com.example.domain.LocalModelManager.formatBytes(downloadState.totalBytes)}",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingGrey
                                    )
                                    Text(
                                        text = "PAUSE",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingGrey,
                                        modifier = Modifier.clickable { modelManager.cancelDownload(model) }
                                    )
                                }
                            }
                        }

                        is com.example.domain.LocalModelManager.DownloadState.Completed -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = NothingRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "READY",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingRed
                                    )
                                    Text(
                                        text = "Remove",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingGrey,
                                        modifier = Modifier.clickable { viewModel.deleteModel(model.id) }
                                    )
                                }
                            }
                        }

                        is com.example.domain.LocalModelManager.DownloadState.Failed -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = NothingRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "FAILED: ${downloadState.error}",
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = NothingRed
                                        )
                                    }
                                }
                                androidx.compose.material3.FilledButton(
                                    onClick = { viewModel.downloadModel(model.id) },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Retry",
                                            tint = NothingBlack,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "RETRY",
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
