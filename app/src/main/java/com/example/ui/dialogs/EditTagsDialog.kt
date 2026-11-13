package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily

enum class ProjectStatus(val tag: String, val label: String) {
    DRAFT("draft", "DRAFT"),
    IN_PROGRESS("in_progress", "IN PROGRESS"),
    READY("ready", "READY"),
    COMPLETED("completed", "COMPLETED")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTagsDialog(
    song: Song,
    onSave: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Extract status tag if present in tags, else default to DRAFT
    var currentStatus by remember {
        val statusFromTag = ProjectStatus.entries.firstOrNull {
            song.tags.contains("status:${it.tag}", ignoreCase = true) ||
                    song.tags.contains(it.tag, ignoreCase = true)
        }
        mutableStateOf(statusFromTag ?: ProjectStatus.DRAFT)
    }

    var selectedGenre by remember { mutableStateOf(song.genre) }
    var selectedMood by remember { mutableStateOf(song.mood) }

    // Parse existing custom tags (excluding status prefix)
    val customTags = remember {
        mutableStateListOf<String>().apply {
            val rawTags = song.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("status:", ignoreCase = true) }
            addAll(rawTags)
        }
    }

    var newTagInput by remember { mutableStateOf("") }

    val popularGenres = listOf("Arabic Pop", "Trap", "Synthwave", "R&B", "Hip-Hop", "EDM", "Acoustic", "Ambient")
    val popularMoods = listOf("Energetic", "Chill", "Melancholic", "Euphoric", "Romantic", "Dark")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, NothingBorder, RoundedCornerShape(24.dp))
                .testTag("edit_tags_dialog"),
            color = NothingBlack
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TAGGING SYSTEM // CATEGORIES",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.title,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 18.sp,
                            color = NothingWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    NothingButton(
                        text = "",
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_tags_dialog"),
                        style = NothingButtonStyle.IconOnly,
                        icon = Icons.Default.Close
                    )
                }

                // 1. Project Status Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PROJECT STATUS",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProjectStatus.entries.forEach { status ->
                            val isSelected = currentStatus == status
                            NothingButton(
                                text = status.label,
                                onClick = { currentStatus = status },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .testTag("status_${status.tag}"),
                                style = if (isSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
                            )
                        }
                    }
                }

                // 2. Genre Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GENRE CLASSIFICATION",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularGenres.forEach { genre ->
                            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                            NothingButton(
                                text = genre.uppercase(),
                                onClick = { selectedGenre = genre },
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("genre_${genre.replace(" ", "_")}"),
                                style = if (isSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
                            )
                        }
                    }
                }

                // 3. Mood Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MOOD CATEGORY",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularMoods.forEach { mood ->
                            val isSelected = selectedMood.equals(mood, ignoreCase = true)
                            NothingButton(
                                text = mood.uppercase(),
                                onClick = { selectedMood = mood },
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("mood_$mood"),
                                style = if (isSelected) NothingButtonStyle.Primary else NothingButtonStyle.Secondary
                            )
                        }
                    }
                }

                // 4. Custom Tags (Add & Delete)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CUSTOM TAGS",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_tag_input"),
                            placeholder = { Text("e.g. Single, Collab, Album 2026...", fontSize = 11.sp, color = NothingGrey) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NothingRed,
                                unfocusedBorderColor = NothingBorder,
                                focusedTextColor = NothingWhite,
                                unfocusedTextColor = NothingWhite,
                                cursorColor = NothingRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        NothingButton(
                            text = "",
                            onClick = {
                                val trimmed = newTagInput.trim().replace(",", "")
                                if (trimmed.isNotBlank() && !customTags.contains(trimmed)) {
                                    customTags.add(trimmed)
                                    newTagInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_custom_tag_button"),
                            style = NothingButtonStyle.Secondary,
                            icon = Icons.Default.Add
                        )
                    }

                    // Flow of current tags
                    if (customTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customTags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NothingDarkGrey)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = NothingWhite
                                    )
                                    NothingButton(
                                        text = "",
                                        onClick = { customTags.remove(tag) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("remove_tag_$tag"),
                                        style = NothingButtonStyle.IconOnly,
                                        icon = Icons.Default.Close
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Save button
                NothingButton(
                    text = "SAVE TAGS & STATUS",
                    onClick = {
                        val combinedTags = mutableListOf<String>()
                        // Add status tag
                        combinedTags.add("status:${currentStatus.tag}")
                        // Add custom tags
                        combinedTags.addAll(customTags)

                        val updatedSong = song.copy(
                            genre = selectedGenre,
                            mood = selectedMood,
                            tags = combinedTags.joinToString(", ")
                        )
                        onSave(updatedSong)
                        Toast.makeText(context, "Tags updated for ${song.title}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_tags_button"),
                    style = NothingButtonStyle.Primary
                )
            }
        }
    }
}