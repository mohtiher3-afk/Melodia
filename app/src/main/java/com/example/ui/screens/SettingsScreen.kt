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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

        // Gemini AI Status Card
        GlyphCard(headerTitle = "GEMINI AI LYRIC ENGINE") {
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
                            text = "Gemini AI API Key",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = if (isGeminiConfigured) "Configured in Environment Secrets" else "Not configured — set GEMINI_API_KEY in .env",
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
                    text = "Select the language model backend for lyric drafting:",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingGrey
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf<Pair<String, String>>(
                                            com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI to "Gemini",
                                            com.example.network.llm.LlmProviderFactory.PROVIDER_OPENROUTER to "FreeLLM"
                    ).forEach { (id, label) ->
                        val isSel = llmProviderId == id
                        val configured = if (id == com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI) isGeminiConfigured else isFreeLlmConfigured
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
                                    text = if (configured) "READY" else "NOT CONFIGURED",
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
                    text = "// Gemini AI Lyric Drafting & Rhyme Engine\n// Local Whisper Audio Synchronization\n// 4K & 1080p MP4 Hardware Lyric Video Exporter",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = NothingGrey
                )
            }
        }
    }
}
