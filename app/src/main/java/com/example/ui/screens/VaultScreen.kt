package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LyricDraft
import com.example.data.model.Song
import com.example.data.model.matchesFilter
import com.example.data.model.projectStatusFromTags
import com.example.ui.components.GlyphCard
import com.example.ui.components.NothingCoverArt
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingOffWhite
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.MelodiaViewModel
import com.example.ui.dialogs.*
import com.example.ui.components.TapHaptic
import com.example.ui.components.CommitHaptic
import com.example.ui.components.pressable
import com.example.ui.screens.PlayerScreen
import com.example.domain.AudioPlayerManager
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import com.example.data.model.CategoryFilterType

val statusOptions = listOf("DRAFT", "IN PROGRESS", "READY", "COMPLETED")
val genreOptions = listOf("Arabic Pop", "Trap", "Synthwave", "R&B", "Hip-Hop", "EDM")
val moodOptions = listOf("Energetic", "Chill", "Melancholic", "Euphoric", "Romantic")

@Composable
fun VaultScreen(
    viewModel: MelodiaViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.savedSongs.collectAsStateWithLifecycle()
    val drafts by viewModel.savedDrafts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val audioPlayerManager = AudioPlayerManager.getInstance(context)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryFilterType.ALL) }
    var selectedSubTag by remember { mutableStateOf<String?>(null) }
    var isBulkEditing by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBulkTagsDialog by remember { mutableStateOf(false) }

    var selectedSongForLyrics by remember { mutableStateOf<Song?>(null) }
    var selectedSongForExport by remember { mutableStateOf<Song?>(null) }
    var selectedSongForTags by remember { mutableStateOf<Song?>(null) }
    var selectedSongForWaveform by remember { mutableStateOf<Song?>(null) }
    var selectedSongForSync by remember { mutableStateOf<Song?>(null) }
    var showPlayerDialog by remember { mutableStateOf(false) }

        val audioPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.importLocalAudio(context, uri)
                Toast.makeText(context, "Importing audio to library...", Toast.LENGTH_SHORT).show()
            }
        }

        val filteredDrafts = drafts.filter { it.matchesFilter(searchQuery, selectedCategory, selectedSubTag) }
        val filteredSongs = songs.filter { it.matchesFilter(searchQuery, selectedCategory, selectedSubTag) }

        @Composable
        fun CategoryFilterChip(
            label: String,
            isSelected: Boolean,
            onSelect: () -> Unit,
            selected: Boolean
        ) {
            NothingButton(
                text = label,
                onClick = onSelect,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = if (isSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
            )
        }

    @Composable
    fun SubTagChip(
        opt: String,
        isOptSelected: Boolean,
        onSelect: () -> Unit
    ) {
        NothingButton(
            text = opt.uppercase(),
            onClick = onSelect,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp),
            style = if (isOptSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
        )
    }

    @Composable
    fun TagButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        NothingButton(
            text = text,
            onClick = onClick,
            modifier = modifier
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = NothingButtonStyle.Secondary
        )
    }

    @Composable
    fun ClearTagButton(
        selectedSubTag: String?,
        onClear: () -> Unit
    ) {
        NothingButton(
            text = stringResource(R.string.vault_tag_hash, selectedSubTag?.uppercase() ?: ""),
            onClick = onClear,
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = NothingButtonStyle.Secondary
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(16.dp)
            .testTag("vault_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.vault_header_kicker),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 11.sp,
                    color = NothingRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isBulkEditing) "SELECT TRACKS" else "SAVED PRODUCTIONS",
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NothingButton(
                    text = stringResource(R.string.vault_action_import),
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("vault_import_audio_button"),
                    style = NothingButtonStyle.Secondary,
                    icon = Icons.Default.Add,
                    iconTint = NothingRed
                )
            }
        }

        // Search Bar with icon & clear button
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_search_input"),
            placeholder = {
                Text(
                    "Search by track name, lyrics, or tag...",
                    color = NothingGrey,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotBlank()) NothingRed else NothingGrey
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    NothingButton(
                        text = "",
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(48.dp),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Close,
                        iconTint = NothingGrey
                    )
                } else {
                    Box(modifier = Modifier.size(0.dp))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NothingRed,
                unfocusedBorderColor = NothingBorder,
                focusedTextColor = NothingWhite,
                unfocusedTextColor = NothingWhite,
                cursorColor = NothingRed
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // 1. Primary Category Selector (ALL, STATUS, GENRE, MOOD, FAVORITES)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryFilterType.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                CategoryFilterChip(
                    label = cat.label,
                    isSelected = isSelected,
                    onSelect = {
                        selectedCategory = cat
                        selectedSubTag = null
                    },
                    selected = isSelected
                )
            }
        }

        // 2. Secondary Sub-Tag Selector (Conditional based on active category)
        val activeSubOptions = when (selectedCategory) {
            CategoryFilterType.STATUS -> statusOptions
            CategoryFilterType.GENRE -> genreOptions
            CategoryFilterType.MOOD -> moodOptions
            else -> emptyList()
        }

        if (activeSubOptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "ALL" in current sub-category
                val isAllSub = selectedSubTag == null
                NothingButton(
                    text = stringResource(R.string.vault_all_tag, selectedCategory.label),
                    onClick = { selectedSubTag = null },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = if (isAllSub) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
                )

                activeSubOptions.forEach { opt ->
                    val isOptSelected = selectedSubTag.equals(opt, ignoreCase = true)
                    SubTagChip(
                        opt = opt,
                        isOptSelected = isOptSelected,
                        onSelect = { selectedSubTag = if (isOptSelected) null else opt }
                    )
                }
            }
        }

        // Active filter indicator if DRAFTS, subTag, or search is active
        if (selectedCategory == CategoryFilterType.DRAFTS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vault_drafts_count, filteredDrafts.size),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingRed,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (selectedSubTag != null || searchQuery.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vault_tracks_matching, filteredSongs.size),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingRed,
                    fontWeight = FontWeight.Bold
                )

                if (selectedSubTag != null) {
                    NothingButton(
                        text = "",
                        onClick = { selectedSubTag = null },
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Close,
                        iconTint = NothingRed
                    )
                }
            }
        }

        // List or Empty State
        if (selectedCategory == CategoryFilterType.DRAFTS) {
            if (filteredDrafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vault_empty_drafts),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 14.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.vault_empty_drafts_help),
                            fontSize = 12.sp,
                            color = NothingGrey,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NothingButton(
                                text = stringResource(R.string.vault_reset_filters),
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = CategoryFilterType.ALL
                                    selectedSubTag = null
                                },
                                style = NothingButtonStyle.Secondary,
                                testTag = "reset_tags_filter_button"
                            )
                            NothingButton(
                                text = stringResource(R.string.vault_action_import_audio),
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                style = NothingButtonStyle.Primary,
                                testTag = "empty_vault_import_audio_button"
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDrafts, key = { it.id }) { draft ->
                        SavedDraftItem(
                            draft = draft,
                            onRestoreToStudio = {
                                viewModel.restoreDraft(draft)
                                onNavigateToStudio()
                                Toast.makeText(context, "Restored draft // ${draft.title}", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteDraft(draft)
                                Toast.makeText(context, "Draft deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        } else if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.vault_empty_tracks),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 14.sp,
                        color = NothingGrey,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.vault_empty_tracks_help),
                        fontSize = 12.sp,
                        color = NothingGrey,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NothingButton(
                            text = stringResource(R.string.vault_reset_filters),
                            onClick = {
                                searchQuery = ""
                                selectedCategory = CategoryFilterType.ALL
                                selectedSubTag = null
                            },
                            style = NothingButtonStyle.Secondary,
                            testTag = "reset_tags_filter_button"
                        )
                        NothingButton(
                            text = stringResource(R.string.vault_action_import_audio),
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            style = NothingButtonStyle.Primary,
                            testTag = "empty_vault_import_audio_button"
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    SavedSongItem(
                        song = song,
                        searchQuery = searchQuery,
                        isBulkEditing = isBulkEditing,
                        isSelected = selectedIds.contains(song.id),
                        onToggleSelection = {
                                                    val newSet = selectedIds.toMutableSet()
                                                    if (selectedIds.contains(song.id)) newSet.remove(song.id) else newSet.add(song.id)
                                                    selectedIds = newSet
                                                },
                        onOpenLyrics = { selectedSongForLyrics = song },
                        onExport = { selectedSongForExport = song },
                        onEditTags = { selectedSongForTags = song },
                        onTagClick = { tag ->
                            selectedCategory = CategoryFilterType.ALL
                            selectedSubTag = tag
                        },
                        onLoadToStudio = {
                            viewModel.loadSongIntoStudio(song)
                            onNavigateToStudio()
                            Toast.makeText(context, "Loaded ${song.title} to Studio", Toast.LENGTH_SHORT).show()
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(song) },
                        onDelete = {
                            viewModel.deleteSong(song)
                            Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                        },
                        onOpenWaveform = { selectedSongForWaveform = song },
                        onOpenSync = { selectedSongForSync = song },
                        onRegenerateCover = { viewModel.regenerateCoverArt(song) },
                        onPlay = {
                            viewModel.playSong(song)
                            showPlayerDialog = true
                        }
                    )
                }
            }
        }

        if (isBulkEditing && selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NothingCardSurface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vault_selected_count, selectedIds.size),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                    color = NothingWhite,
                    fontWeight = FontWeight.Bold
                )

                NothingButton(
                    text = stringResource(R.string.vault_edit_tags),
                    onClick = { showBulkTagsDialog = true },
                    style = NothingButtonStyle.Primary,
                    testTag = "bulk_edit_tags_button"
                )
            }
        }
    }

    // Full-screen Player Dialog
    if (showPlayerDialog) {
        Dialog(
            onDismissRequest = { showPlayerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PlayerScreen(
                audioPlayerManager = audioPlayerManager,
                onDismiss = { showPlayerDialog = false }
            )
        }
    }
}

@Composable
fun SavedDraftItem(
    draft: LyricDraft,
    onRestoreToStudio: () -> Unit,
    onDelete: () -> Unit
) {
    // Implementation...
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedSongItem(
    song: Song,
    searchQuery: String,
    isBulkEditing: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onOpenLyrics: () -> Unit,
    onExport: () -> Unit,
    onEditTags: () -> Unit,
    onTagClick: (String) -> Unit,
    onLoadToStudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenWaveform: () -> Unit,
    onOpenSync: () -> Unit = {},
    onRegenerateCover: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val formattedDate = remember(song.createdAt) { dateFormat.format(Date(song.createdAt)) }

    // Detect status tag
    val projectStatus = remember(song.tags) {
        song.projectStatusFromTags(song.tags)
    }

    // Extract custom tags
    val customTagsList: List<String> = remember(song.tags) {
        song.tags.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("status:", ignoreCase = true) }
    }

    // Detect if search query matched inside lyrics
    val matchedLyricSnippet = remember(song.lyrics, searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2 && song.lyrics.contains(q, ignoreCase = true)) {
            val matchingLine = song.lyrics.lines()
                .firstOrNull { it.contains(q, ignoreCase = true) && !it.startsWith("[") }
                ?: song.lyrics.lines().firstOrNull { it.contains(q, ignoreCase = true) }
            matchingLine?.trim()
        } else null
    }

    GlyphCard(
        testTag = "saved_song_${song.id}",
        modifier = Modifier.pressable(
            pressedScale = 0.94f,
            haptic = com.example.ui.components.TapHaptic,
            enabled = isBulkEditing,
            onClick = { onToggleSelection() }
        ),
        content = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBulkEditing) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NothingRed else NothingCardSurface)
                                .border(1.dp, NothingBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = NothingWhite, fontSize = 14.sp)
                            }
                        }
                    }

                    NothingCoverArt(
                        title = song.title,
                        genre = song.genre,
                        imagePath = song.coverImagePath,
                        modifier = Modifier.size(72.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (song.isLocal) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NothingWhite.copy(alpha = 0.2f))
                                            .border(1.dp, NothingWhite, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.vault_track_local_badge),
                                            fontFamily = SpaceMonoFontFamily,
                                            fontSize = 8.sp,
                                            color = NothingWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                val statusColor = when (projectStatus) {
                                    "DRAFT" -> NothingRed
                                    "IN PROGRESS" -> NothingGrey
                                    "READY" -> NothingWhite
                                    "COMPLETED" -> NothingRed
                                    else -> NothingGrey
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.2f))
                                        .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = projectStatus,
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 8.sp,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = formattedDate,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 9.sp,
                                color = NothingGrey
                            )
                        }

                        Text(
                            text = song.title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 15.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Text(
                            text = song.promptText,
                            fontSize = 11.sp,
                            color = NothingGrey,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NothingButton(
                        text = song.genre.ifBlank { "UNLABELED" },
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = NothingButtonStyle.Quiet,
                        enabled = false
                    )

                    NothingButton(
                        text = song.mood.ifBlank { "NO MOOD" },
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = NothingButtonStyle.Quiet,
                        enabled = false
                    )

                    customTagsList.forEach { tag ->
                        NothingButton(
                            text = tag,
                            onClick = { onTagClick(tag) },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = NothingButtonStyle.Secondary
                        )
                    }
                }

                if (matchedLyricSnippet != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.vault_matched_snippet, matchedLyricSnippet.take(120) + if (matchedLyricSnippet.length > 120) "..." else ""),
                        fontSize = 12.sp,
                        color = NothingGrey,
                        maxLines = 2
                    )
                }
            }
        }
    )
}
