package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.domain.LocalModelManager
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
    val systemIslandEnabled by viewModel.systemIslandEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var enableIslandAfterPermission by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                if (enableIslandAfterPermission) {
                    enableIslandAfterPermission = false
                    if (hasOverlayPermission) viewModel.setSystemIslandEnabled(true)
                } else if (!hasOverlayPermission && systemIslandEnabled) {
                    viewModel.setSystemIslandEnabled(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                    text = "Nano runs on-device on supported phones (AICore may download its model once). Gemma 3 runs fully offline after a Settings download. Cloud options may have quotas or charges:",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
                val nanoStatus by viewModel.nanoStatus.collectAsStateWithLifecycle()
                val gemmaDownloaded = remember {
                    val info = LocalModelManager.getModel("gemma3_1b")
                    info != null && LocalModelManager.getInstance(context).isModelDownloaded(info)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderTile(
                        id = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
                        label = "Nano Local",
                        badge = when {
                            !com.example.network.llm.GeminiNanoLlmProvider().isAvailable() -> "NOT SUPPORTED"
                            nanoStatus == null -> "CHECKING"
                            nanoStatus == com.google.mlkit.genai.common.FeatureStatus.AVAILABLE -> "READY"
                            nanoStatus == com.google.mlkit.genai.common.FeatureStatus.DOWNLOADING -> "DOWNLOADING"
                            nanoStatus == com.google.mlkit.genai.common.FeatureStatus.DOWNLOADABLE -> "NEEDS DOWNLOAD"
                            else -> "NOT SUPPORTED"
                        },
                        isSel = llmProviderId == com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO,
                        onClick = { viewModel.updateLlmProvider(com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderTile(
                        id = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI,
                        label = "Gemini API",
                        badge = if (isGeminiConfigured) "CONFIGURED" else "NOT CONFIGURED",
                        isSel = llmProviderId == com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI,
                        onClick = { viewModel.updateLlmProvider(com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderTile(
                        id = com.example.network.llm.LlmProviderFactory.PROVIDER_GEMMA3_LOCAL,
                        label = "Gemma 3",
                        badge = if (gemmaDownloaded) "READY" else "NOT DOWNLOADED",
                        isSel = llmProviderId == com.example.network.llm.LlmProviderFactory.PROVIDER_GEMMA3_LOCAL,
                        onClick = { viewModel.updateLlmProvider(com.example.network.llm.LlmProviderFactory.PROVIDER_GEMMA3_LOCAL) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderTile(
                        id = com.example.network.llm.LlmProviderFactory.PROVIDER_OPENROUTER,
                        label = "Other Cloud",
                        badge = if (isFreeLlmConfigured) "CONFIGURED" else "NOT CONFIGURED",
                        isSel = llmProviderId == com.example.network.llm.LlmProviderFactory.PROVIDER_OPENROUTER,
                        onClick = { viewModel.updateLlmProvider(com.example.network.llm.LlmProviderFactory.PROVIDER_OPENROUTER) },
                        modifier = Modifier.weight(1f)
                    )
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

        GlyphCard(headerTitle = "SYSTEM PLAYBACK ISLAND") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Floating playback controls",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = when {
                            systemIslandEnabled -> "Enabled. The Island appears while a track is active."
                            hasOverlayPermission -> "Off by default. You can turn it on at any time."
                            else -> "Off by default. Android asks for display-over-apps access only if enabled."
                        },
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
                    )
                }
                Switch(
                    checked = systemIslandEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setSystemIslandEnabled(false)
                        } else if (hasOverlayPermission) {
                            viewModel.setSystemIslandEnabled(true)
                        } else {
                            enableIslandAfterPermission = true
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("system_island_switch")
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
    // Shared singleton: the same instance the ViewModel downloads with, so the
    // progress observed here is the actual transfer (previously this created its
    // own manager and progress/pause acted on a different one).
    val modelManager = viewModel.localModelManager

    Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.example.domain.LocalModelManager.AVAILABLE_MODELS.forEach { model ->
                            val downloadStateLiveData = modelManager.downloadStates[model.id]
                            val downloadState by downloadStateLiveData?.observeAsState()
                                ?: remember { mutableStateOf<com.example.domain.LocalModelManager.DownloadState?>(null) }

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

                                    // Use local variable for smart cast
                                                                        val state = downloadState
                                                                        when (state) {
                                                                            null -> {
                                                                                // Initial state, show loading or nothing
                                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                            }
                                                                            is com.example.domain.LocalModelManager.DownloadState.Idle -> {
                                                                                if (state.isModelPresent) {
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
                                                                                            text = "Size: ${LocalModelManager.formatBytes(model.expectedSizeBytes)}",
                                                                                            fontFamily = SpaceMonoFontFamily,
                                                                                            fontSize = 10.sp,
                                                                                            color = NothingGrey
                                                                                        )
                                                                                        Button(
                                                                                            onClick = { viewModel.downloadModel(model.id) },
                                                                                            modifier = Modifier.testTag("download_${model.id}"),
                                                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                                                containerColor = NothingRed,
                                                                                                contentColor = NothingBlack
                                                                                            )
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
                                                                                                                LinearProgressIndicator(
                                                                                                                    progress = state.progress,
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
                                                                                                                        text = "${(state.progress * 100).toInt()}% • ${LocalModelManager.formatBytes(state.downloadedBytes)} / ${LocalModelManager.formatBytes(state.totalBytes)}",
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

                                                is com.example.domain.LocalModelManager.DownloadState.Paused -> {
                                                                                                    Row(
                                                                                                        modifier = Modifier.fillMaxWidth(),
                                                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                                                    ) {
                                                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                                                            Text(
                                                                                                                text = "PAUSED: ${LocalModelManager.formatBytes(state.downloadedBytes)} / ${LocalModelManager.formatBytes(state.totalBytes)}",
                                                                                                                fontFamily = SpaceMonoFontFamily,
                                                                                                                fontSize = 10.sp,
                                                                                                                color = NothingGrey
                                                                                                            )
                                                                                                        }
                                                                                                        Button(
                                                                                                            onClick = { viewModel.downloadModel(model.id) },
                                                                                                            modifier = Modifier.padding(top = 4.dp),
                                                                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                                                                containerColor = NothingRed,
                                                                                                                contentColor = NothingBlack
                                                                                                            )
                                                                                                        ) {
                                                                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                                                                Icon(
                                                                                                                    imageVector = Icons.Default.ArrowDownward,
                                                                                                                    contentDescription = "Resume",
                                                                                                                    tint = NothingBlack,
                                                                                                                    modifier = Modifier.size(16.dp)
                                                                                                                )
                                                                                                                Text(
                                                                                                                    text = "RESUME",
                                                                                                                    fontFamily = SpaceMonoFontFamily,
                                                                                                                    fontSize = 11.sp,
                                                                                                                    fontWeight = FontWeight.Bold,
                                                                                                                    color = NothingBlack
                                                                                                                )
                                                                                                            }
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
                                                                                                                                            text = "FAILED: ${state.error}",
                                                                                                                                            fontFamily = SpaceMonoFontFamily,
                                                                                                                                            fontSize = 10.sp,
                                                                                                                                            color = NothingRed
                                                                                                                                        )
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                Button(
                                                                                                                                                                    onClick = { viewModel.downloadModel(model.id) },
                                                                                                                                                                    modifier = Modifier.padding(top = 4.dp),
                                                                                                                                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                                                                                                                                        containerColor = NothingRed,
                                                                                                                                                                        contentColor = NothingBlack
                                                                                                                                                                    )
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

@Composable
private fun ProviderTile(
    id: String,
    label: String,
    badge: String,
    isSel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSel) NothingRed else NothingCardSurface)
            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
            .pressable(
                pressedScale = 0.95f,
                haptic = com.example.ui.components.TapHaptic
            ) { onClick() },
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
                text = badge,
                fontFamily = SpaceMonoFontFamily,
                fontSize = 9.sp,
                color = if (isSel) NothingBlack else NothingGrey
            )
        }
    }
}
