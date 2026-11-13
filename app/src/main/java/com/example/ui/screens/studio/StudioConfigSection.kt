package com.example.ui.screens.studio

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlyphCard
import com.example.ui.NothingButton
import com.example.ui.NothingButtonStyle
import com.example.domain.SentimentAnalyzer
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.SpaceMonoFontFamily
import com.example.ui.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioConfigSection(
    state: StudioUiState,
    onSetGenre: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetMoodKeywords: (String) -> Unit,
    onSetVocalStyle: (String) -> Unit,
    onSetSelectedBpm: (String) -> Unit,
    onSetVocalReverb: (String) -> Unit,
    onOpenGenreDirectory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var genreExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val genres = listOf(
        "Arabic Pop", "Trap", "Mahraganat", "Synthwave", "Lo-Fi",
        "R&B", "Rock", "Acoustic", "Hip Hop", "Classical Tarab",
        "Electronic Dance", "Cinematic Orchestral"
    )
    val languages = listOf("English", "Arabic", "Bilingual")
    val quickMoodKeywords = listOf("Cyberpunk", "Late Night", "Nostalgic", "Euphoric", "Melancholy", "Rebellious", "Hypnotic", "Cinematic")
    val vocalStyles = listOf("Auto-Tuned Male", "Soft Female Vocal", "Duet / Dual Vocals", "Whisper Vocal", "Energetic Rap")
    val bpmOptions = listOf("80 BPM", "100 BPM", "120 BPM", "140 BPM", "160 BPM")
    val reverbOptions = listOf("Studio Clean", "Lush Reverb", "Cathedral Echo", "Subtle Dry")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dropdowns Section: Music Genre & Language
        GlyphCard(headerTitle = "CONFIGURATION // DROPDOWNS") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Music Genre Dropdown
                ExposedDropdownMenuBox(
                    expanded = genreExpanded,
                    onExpandedChange = { genreExpanded = it },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("genre_dropdown_box")
                ) {
                    OutlinedTextField(
                        value = state.selectedGenre,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                "GENRE",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingGrey
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("genre_dropdown_input")
                    )

                    ExposedDropdownMenu(
                        expanded = genreExpanded,
                        onDismissRequest = { genreExpanded = false },
                        modifier = Modifier
                            .background(NothingDarkSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    ) {
                        genres.forEach { genre ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = genre.uppercase(),
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (genre == state.selectedGenre) FontWeight.Bold else FontWeight.Normal,
                                        color = if (genre == state.selectedGenre) NothingRed else NothingWhite
                                    )
                                },
                                onClick = {
                                    onSetGenre(genre)
                                    genreExpanded = false
                                },
                                modifier = Modifier.testTag("genre_item_${genre.replace(" ", "_")}")
                            )
                        }
                    }
                }

                // Language Dropdown (English / Arabic / Bilingual)
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("language_dropdown_box")
                ) {
                    OutlinedTextField(
                        value = state.selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                "LANGUAGE",
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = NothingGrey
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = NothingBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("language_dropdown_input")
                    )

                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false },
                        modifier = Modifier
                            .background(NothingDarkSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang.uppercase(),
                                        fontFamily = SpaceMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (lang == state.selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                        color = if (lang == state.selectedLanguage) NothingRed else NothingWhite
                                    )
                                },
                                onClick = {
                                    onSetLanguage(lang)
                                    languageExpanded = false
                                },
                                modifier = Modifier.testTag("language_item_$lang")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick button to open full Genre & Subgenre Directory Dialog with Recently Viewed section
            NothingButton(
                text = stringResource(R.string.studio_subgenres_header),
                onClick = { onOpenGenreDirectory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_genre_directory_button"),
                style = NothingButtonStyle.Secondary
            )
        }

        // User-Defined Mood Keywords Input
        GlyphCard(headerTitle = "MOOD KEYWORDS // AI LYRICS GENERATION") {
            OutlinedTextField(
                value = state.moodKeywords,
                onValueChange = { onSetMoodKeywords(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_keywords_input"),
                placeholder = {
                    Text(
                        "e.g. Cyberpunk, late-night melancholy, rebellious, euphoria...",
                        color = NothingGrey,
                        fontSize = 13.sp
                    )
                },
                label = {
                    Text(
                        "MOOD KEYWORDS",
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
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
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick select mood keywords chips
            Text(
                text = stringResource(R.string.studio_quick_suggestions),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 9.sp,
                color = NothingGrey,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickMoodKeywords.forEach { keyword ->
                    val isPresent = state.moodKeywords.contains(keyword, ignoreCase = true)
                    NothingButton(
                        text = stringResource(R.string.studio_keyword_hash, keyword),
                        onClick = {
                            val current = state.moodKeywords.trim()
                            val updated = if (current.isBlank()) {
                                keyword
                            } else if (!isPresent) {
                                "$current, $keyword"
                            } else {
                                current
                            }
                            onSetMoodKeywords(updated)
                        },
                        style = if (isPresent) NothingButtonStyle.Primary else NothingButtonStyle.Secondary,
                        modifier = Modifier.testTag("mood_chip_$keyword")
                    )
                }
            }
        }

        // Vocal Style Dropdown
        GlyphCard(headerTitle = "VOCAL STYLE") {
            var vocalStyleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = vocalStyleExpanded,
                onExpandedChange = { vocalStyleExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.vocalStyle,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            "VOCAL STYLE",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingGrey
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = vocalStyleExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("vocal_style_dropdown_input")
                )

                ExposedDropdownMenu(
                    expanded = vocalStyleExpanded,
                    onDismissRequest = { vocalStyleExpanded = false },
                    modifier = Modifier
                        .background(NothingDarkSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                ) {
                    vocalStyles.forEach { style ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = style.uppercase(),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (style == state.vocalStyle) FontWeight.Bold else FontWeight.Normal,
                                    color = if (style == state.vocalStyle) NothingRed else NothingWhite
                                )
                            },
                            onClick = {
                                onSetVocalStyle(style)
                                vocalStyleExpanded = false
                            },
                            modifier = Modifier.testTag("vocal_style_item_${style.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // BPM Dropdown
        GlyphCard(headerTitle = "BPM") {
            var bpmExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = bpmExpanded,
                onExpandedChange = { bpmExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.selectedBpm,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            "BPM",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingGrey
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = bpmExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("bpm_dropdown_input")
                )

                ExposedDropdownMenu(
                    expanded = bpmExpanded,
                    onDismissRequest = { bpmExpanded = false },
                    modifier = Modifier
                        .background(NothingDarkSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                ) {
                    bpmOptions.forEach { bpm ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = bpm,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (bpm == state.selectedBpm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (bpm == state.selectedBpm) NothingRed else NothingWhite
                                )
                            },
                            onClick = {
                                onSetSelectedBpm(bpm)
                                bpmExpanded = false
                            },
                            modifier = Modifier.testTag("bpm_item_$bpm")
                        )
                    }
                }
            }
        }

        // Reverb Dropdown
        GlyphCard(headerTitle = "VOCAL REVERB") {
            var reverbExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = reverbExpanded,
                onExpandedChange = { reverbExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.vocalReverb,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            "REVERB",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            color = NothingGrey
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = reverbExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NothingRed,
                        unfocusedBorderColor = NothingBorder,
                        focusedTextColor = NothingWhite,
                        unfocusedTextColor = NothingWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("reverb_dropdown_input")
                )

                ExposedDropdownMenu(
                    expanded = reverbExpanded,
                    onDismissRequest = { reverbExpanded = false },
                    modifier = Modifier
                        .background(NothingDarkSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                ) {
                    reverbOptions.forEach { reverb ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = reverb.uppercase(),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (reverb == state.vocalReverb) FontWeight.Bold else FontWeight.Normal,
                                    color = if (reverb == state.vocalReverb) NothingRed else NothingWhite
                                )
                            },
                            onClick = {
                                onSetVocalReverb(reverb)
                                reverbExpanded = false
                            },
                            modifier = Modifier.testTag("reverb_item_${reverb.replace(" ", "_")}")
                        )
                    }
                }
            }
        }
    }
}