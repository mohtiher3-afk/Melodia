package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LyricsExportManager
import com.example.ui.components.GlyphCard
import com.example.ui.components.NothingCoverArt
import com.example.ui.components.NothingPillButton
import com.example.ui.dialogs.DraftsDialog
import com.example.ui.dialogs.GenreSelectionDialog
import com.example.ui.dialogs.SavePresetDialog
import com.example.ui.screens.studio.StudioConfigSection
import com.example.ui.screens.studio.StudioHeader
import com.example.ui.screens.studio.StudioPresetsSection
import com.example.ui.screens.studio.StudioTopicSection
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val drafts by viewModel.savedDrafts.collectAsStateWithLifecycle()
    val presets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val recentSubGenres by viewModel.recentSubGenres.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDraftsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showGenreDirectoryDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLocalAudio(context, uri)
            Toast.makeText(context, "Importing local audio track...", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("studio_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        StudioHeader(
            state = state,
            draftsCount = drafts.size,
            onImportAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
            onOpenDrafts = { showDraftsDialog = true },
            onOpenQueue = { showQueueDialog = true }
        )

        // Dashboard Content
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Lead with the primary task; keep the creative controls immediately visible.
            StudioTopicSection(
                state = state,
                onSetUserTopic = { viewModel.setUserTopic(it) },
                onGenerateLyrics = { viewModel.generateLyricsOnly() },
                onGeneratePrompt = { viewModel.generateMusicPromptOnly() }
            )

            // Secondary configuration follows the primary writing action.
            StudioConfigSection(
                state = state,
                onSetGenre = { viewModel.setGenre(it) },
                onSetLanguage = { viewModel.setLanguage(it) },
                onSetMoodKeywords = { viewModel.setMoodKeywords(it) },
                onSetVocalStyle = { viewModel.setVocalStyle(it) },
                onSetSelectedBpm = { viewModel.setSelectedBpm(it) },
                onSetVocalReverb = { viewModel.setVocalReverb(it) },
                onOpenGenreDirectory = { showGenreDirectoryDialog = true }
            )

            // Active Draft Overview Card
            val draft = state.currentDraft
            if (draft != null) {
                GlyphCard(headerTitle = "CURRENT SONG DRAFT OVERVIEW") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = draft.title,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NothingBorder)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = draft.language,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = NothingRed
                                )
                            }
                        }

                        // Copy Prompt & Lyrics Button
                                                NothingPillButton(
                                                    text = "COPY PROMPT & LYRICS",
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val copyText = "PROMPT: ${draft.promptText}\n\nLYRICS: ${draft.lyrics}"
                                                        val clipData = ClipData.newPlainText("melodia_draft", copyText)
                                                        clipboard.setPrimaryClip(clipData)
                                                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    isRedAccent = true
                                                )

                        // Cover art preview
                        NothingCoverArt(
                            title = draft.title,
                            genre = draft.genre,
                            imagePath = draft.coverImagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )

                        if (draft.promptText.isNotBlank()) {
                            Text(
                                text = "Music Prompt: ${draft.promptText}",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 11.sp,
                                color = NothingGrey
                            )
                        }
                    }
                }
            }

            // Presets
            StudioPresetsSection(
                state = state,
                presets = presets,
                onOpenSavePresetDialog = { showSavePresetDialog = true },
                onLoadPreset = { viewModel.loadPreset(it) }
            )
        }
    }

    if (showDraftsDialog) {
        DraftsDialog(
            drafts = drafts,
            activeDraftId = state.activeDraftId,
            onRestoreDraft = {
                viewModel.restoreDraft(it)
                showDraftsDialog = false
            },
            onDeleteDraft = { viewModel.deleteDraft(it) },
            onClearAllDrafts = { viewModel.clearAllDrafts() },
            onDismiss = { showDraftsDialog = false }
        )
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            onSave = { name ->
                viewModel.savePreset(name)
                showSavePresetDialog = false
            },
            onDismiss = { showSavePresetDialog = false }
        )
    }

    if (showGenreDirectoryDialog) {
        GenreSelectionDialog(
            selectedGenre = state.selectedGenre,
            recentSubGenres = recentSubGenres,
            onGenreSelected = { name, category, tempoRange ->
                viewModel.recordSubGenreSelection(name, category, tempoRange)
                showGenreDirectoryDialog = false
            },
            onDismiss = { showGenreDirectoryDialog = false }
        )
    }

    if (showQueueDialog) {
        Dialog(
            onDismissRequest = { showQueueDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NothingBlack)
            ) {
                QueueScreen(
                    viewModel = viewModel,
                    onNavigateToStudio = { showQueueDialog = false },
                    onClose = { showQueueDialog = false }
                )
            }
        }
    }
}
