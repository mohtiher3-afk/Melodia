package com.example.ui.screens.studio

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.pressable
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingBlack)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .clickable { onOpenGenreDirectory() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("open_genre_directory_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Text(
                        text = stringResource(R.string.studio_subgenres_header),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.studio_browse_more),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 10.sp,
                    color = NothingRed,
                    fontWeight = FontWeight.Bold
                )
            }
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPresent) NothingRed else NothingBlack)
                            .border(1.dp, if (isPresent) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val current = state.moodKeywords.trim()
                                val updated = if (current.isBlank()) {
                                    keyword
                                } else if (!isPresent) {
                                    "$current, $keyword"
                                } else {
                                    current
                                }
                                onSetMoodKeywords(updated)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("mood_chip_$keyword")
                    ) {
                        Text(
                            text = stringResource(R.string.studio_keyword_hash, keyword),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPresent) NothingWhite else NothingGrey
                        )
                    }
                }
            }
        }

        // Advanced Prompt Parameters (Vocal Style, BPM, Reverb Space)
        GlyphCard(headerTitle = "VOCAL & TEMPO PARAMETERS // PROMPT CONTROL") {
            // Vocal Style Chips
            Text(
                text = stringResource(R.string.studio_vocal_style),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingGrey
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                vocalStyles.forEach { style ->
                    val isSelected = state.vocalStyle == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NothingRed else NothingBlack)
                            .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                            .pressable(
                                pressedScale = 0.93f,
                                haptic = com.example.ui.components.TapHaptic
                            ) { onSetVocalStyle(style) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("vocal_style_chip_${style.replace(" ", "_")}")
                    ) {
                        Text(
                            text = style.uppercase(),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NothingWhite else NothingGrey
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BPM Selector Chips
            Text(
                text = stringResource(R.string.studio_tempo_bpm),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingGrey
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bpmOptions.forEach { bpm ->
                    val isSelected = state.selectedBpm == bpm
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NothingRed else NothingBlack)
                            .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                            .pressable(
                                pressedScale = 0.93f,
                                haptic = com.example.ui.components.TapHaptic
                            ) { onSetSelectedBpm(bpm) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("bpm_chip_${bpm.replace(" ", "_")}")
                    ) {
                        Text(
                            text = bpm,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NothingWhite else NothingGrey
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reverb Space Chips
            Text(
                text = stringResource(R.string.studio_reverb_space),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingGrey
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                reverbOptions.forEach { reverb ->
                    val isSelected = state.vocalReverb == reverb
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NothingRed else NothingBlack)
                            .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                            .pressable(
                                pressedScale = 0.93f,
                                haptic = com.example.ui.components.TapHaptic
                            ) { onSetVocalReverb(reverb) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("reverb_chip_${reverb.replace(" ", "_")}")
                    ) {
                        Text(
                            text = reverb.uppercase(),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NothingWhite else NothingGrey
                        )
                    }
                }
            }
        }

        // Sentiment Analysis Chip
        GlyphCard(headerTitle = "SENTIMENT ANALYSIS") {
            var analyzePressed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (state.sentimentResult != null) {
                            NothingRed.copy(alpha = 0.15f)
                        } else {
                            NothingCardSurface
                        }
                    )
                    .border(1.dp, if (state.sentimentResult != null) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                    .pressable(
                        pressedScale = 0.95f,
                        haptic = com.example.ui.components.TapHaptic
                    ) { analyzePressed = !analyzePressed }
                    .testTag("sentiment_analyze_chip")
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.sentimentResult == null && !analyzePressed) {
                        Text(
                            text = stringResource(R.string.studio_analyze_sentiment),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingGrey
                        )
                    } else if (analyzePressed) {
                        Text(
                            text = stringResource(R.string.studio_analyzing),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    } else if (state.sentimentResult != null) {
                        val r = state.sentimentResult
                        val dominant = r.dominantTone
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.studio_sentiment_dominant, dominant),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingRed
                            )
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = stringResource(R.string.studio_sentiment_joy, r.joyScore),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = NothingWhite
                                )
                                Text(
                                    text = stringResource(R.string.studio_sentiment_energy, r.energyScore),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = NothingWhite
                                )
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = stringResource(R.string.studio_sentiment_melancholy, r.melancholyScore),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = NothingWhite
                                )
                                Text(
                                    text = stringResource(R.string.studio_sentiment_nostalgia, r.nostalgiaScore),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = NothingWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
