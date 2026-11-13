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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import com.example.R
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
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.nothing.staggeredEnter
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
        Column(modifier = Modifier.staggeredEnter(1)) {
            Text(
                text = stringResource(R.string.settings_header_kicker),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                color = NothingRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = stringResource(R.string.settings_title),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NothingWhite
            )
        }

        // Cloud key status (not required by the local provider)
        GlyphCard(headerTitle = "GEMINI CLOUD API", modifier = Modifier.staggeredEnter(2)) {
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
                            text = stringResource(R.string.settings_gemini_api_key_label),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = if (isGeminiConfigured) stringResource(R.string.settings_gemini_configured_note) else stringResource(R.string.settings_gemini_not_configured_note),
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
        GlyphCard(headerTitle = "LLM PROVIDER", modifier = Modifier.staggeredEnter(3)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_provider_help),
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
                        GlyphCard(headerTitle = "LOCAL MODEL DOWNLOADS", modifier = Modifier.staggeredEnter(4)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_model_downloads_help),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = NothingGrey
                                )
                                LocalModelDownloadSection(viewModel = viewModel)
                            }
                        }

                        // Local Audio Privacy Card
                        GlyphCard(headerTitle = "LOCAL AUDIO PRIVACY", modifier = Modifier.staggeredEnter(5)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_allow_cloud_transcription),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = if (allowCloudTranscription) stringResource(R.string.settings_cloud_transcription_on) else stringResource(R.string.settings_cloud_transcription_off),
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

        GlyphCard(headerTitle = "SYSTEM PLAYBACK ISLAND", modifier = Modifier.staggeredEnter(6)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_floating_playback_controls),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = when {
                            systemIslandEnabled -> stringResource(R.string.settings_island_enabled)
                            hasOverlayPermission -> stringResource(R.string.settings_island_off_default)
                            else -> stringResource(R.string.settings_island_off_permission)
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
                    text = stringResource(R.string.settings_timing_format_help),
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
                        NothingButton(
                            text = fmt,
                            onClick = { viewModel.updateDefaultExportFormat(fmt) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp),
                            style = if (isSel) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
                            enabled = true
                        )
                    }
                }
            }
        }

        // About & Version Info
        GlyphCard(headerTitle = "MELODIA STUDIO ARCHITECTURE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_about_title),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
                Text(
                    text = stringResource(R.string.settings_about_body),
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
                                                                                                text = stringResource(R.string.model_state_ready),
                                                                                                fontFamily = SpaceMonoFontFamily,
                                                                                                fontSize = 12.sp,
                                                                                                fontWeight = FontWeight.Bold,
                                                                                                color = NothingRed
                                                                                            )
                                                            NothingButton(
                                                                text = stringResource(R.string.model_action_remove),
                                                                onClick = { viewModel.deleteModel(model.id) },
                                                                style = NothingButtonStyle.Quiet
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
                                                                                            text = stringResource(R.string.model_size_format, LocalModelManager.formatBytes(model.expectedSizeBytes)),
                                                                                            fontFamily = SpaceMonoFontFamily,
                                                                                            fontSize = 10.sp,
                                                                                            color = NothingGrey
                                                                                        )
                                                                                        NothingButton(
                                                                                            text = stringResource(R.string.model_action_download),
                                                                                            onClick = { viewModel.downloadModel(model.id) },
                                                                                            modifier = Modifier.testTag("download_${model.id}"),
                                                                                            style = NothingButtonStyle.Primary,
                                                                                            icon = Icons.Default.ArrowDownward
                                                                                        )
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
                                                                                                                        text = stringResource(R.string.model_download_progress, (state.progress * 100).toInt().toString(), LocalModelManager.formatBytes(state.downloadedBytes), LocalModelManager.formatBytes(state.totalBytes)),
                                                                                                                        fontFamily = SpaceMonoFontFamily,
                                                                                                                        fontSize = 10.sp,
                                                                                                                        color = NothingGrey
                                                                                                                    )
                                                                                                                    Text(
                                                                                                                        text = stringResource(R.string.model_action_pause),
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
                                                                text = stringResource(R.string.model_state_ready),
                                                                fontFamily = SpaceMonoFontFamily,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NothingRed
                                                            )
                                                            NothingButton(
                                                                text = stringResource(R.string.model_action_remove),
                                                                onClick = { viewModel.deleteModel(model.id) },
                                                                style = NothingButtonStyle.Quiet
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
                                                                                                                text = stringResource(R.string.model_state_paused, LocalModelManager.formatBytes(state.downloadedBytes), LocalModelManager.formatBytes(state.totalBytes)),
                                                                                                                fontFamily = SpaceMonoFontFamily,
                                                                                                                fontSize = 10.sp,
                                                                                                                color = NothingGrey
                                                                                                            )
                                                                                                        }
                                                                                                        NothingButton(
                                                                                                            text = stringResource(R.string.model_action_resume),
                                                                                                            onClick = { viewModel.downloadModel(model.id) },
                                                                                                            modifier = Modifier.padding(top = 4.dp),
                                                                                                            style = NothingButtonStyle.Primary,
                                                                                                            icon = Icons.Default.ArrowDownward
                                                                                                        )
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
                                                                                                                                            text = stringResource(R.string.model_state_failed, state.error),
                                                                                                                                            fontFamily = SpaceMonoFontFamily,
                                                                                                                                            fontSize = 10.sp,
                                                                                                                                            color = NothingRed
                                                                                                                                        )
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                NothingButton(
                                                                                                                                                                    text = stringResource(R.string.model_action_retry),
                                                                                                                                                                    onClick = { viewModel.downloadModel(model.id) },
                                                                                                                                                                    modifier = Modifier.padding(top = 4.dp),
                                                                                                                                                                    style = NothingButtonStyle.Primary,
                                                                                                                                                                    icon = Icons.Default.ArrowDownward
                                                                                                                                                                )
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
    NothingButton(
        text = label,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        style = if (isSel) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
        enabled = true
    )
}
