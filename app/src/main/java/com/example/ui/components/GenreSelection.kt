package com.example.ui.components

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecentSubGenre
import com.example.ui.MelodiaCard
import com.example.ui.MelodiaTheme
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingDarkGrey
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingDotMatrixFontFamily
import com.example.ui.theme.NothingGrey
import com.example.ui.theme.SpaceMonoFontFamily

data class SubGenre(
    val name: String,
    val category: String,
    val tempoRange: String = "",
    val characteristics: String = ""
)

/**
 * Master framework categorized by genres, sub-genres, production signatures, and BPM ranges.
 */
val MASTER_SUB_GENRES = listOf(
    // 1. R&B & Soul Sub-Genres
    SubGenre(
        name = "Classic R&B & Soul",
        category = "R&B & Soul",
        tempoRange = "75 – 100 BPM",
        characteristics = "Live instrumentation, bass guitar, horn sections, layered vocal harmonies"
    ),
    SubGenre(
        name = "90s Neo-Soul & Contemporary R&B",
        category = "R&B & Soul",
        tempoRange = "80 – 105 BPM",
        characteristics = "Warm electric guitar riffs, deep sub-bass, rhodes keys, subtle vinyl crackle"
    ),
    SubGenre(
        name = "Modern R&B / Pop-R&B Crossover",
        category = "R&B & Soul",
        tempoRange = "105 – 115 BPM",
        characteristics = "Atmospheric synth pads, tight 808 bass, crisp hi-hats, clean electric guitar"
    ),
    SubGenre(
        name = "PBR&B / Alternative R&B",
        category = "R&B & Soul",
        tempoRange = "65 – 95 BPM",
        characteristics = "Dark, atmospheric synth textures, filtered drums, heavy reverb/delay"
    ),

    // 2. Hip-Hop & Classic Rap
    SubGenre(
        name = "East Coast Boom Bap",
        category = "Hip-Hop & Classic Rap",
        tempoRange = "85 – 95 BPM",
        characteristics = "Heavy snare/kick patterns, chopped jazz/soul vinyl samples, prominent basslines"
    ),
    SubGenre(
        name = "West Coast G-Funk",
        category = "Hip-Hop & Classic Rap",
        tempoRange = "90 – 100 BPM",
        characteristics = "Whistling synthesizer leads, funky basslines, talk-box vocals"
    ),
    SubGenre(
        name = "Conscious / Jazz Rap",
        category = "Hip-Hop & Classic Rap",
        tempoRange = "85 – 98 BPM",
        characteristics = "Live bass, saxophone/piano samples, intricate lyricism, organic drum breaks"
    ),
    SubGenre(
        name = "Melodic Rap / Sing-Rap",
        category = "Hip-Hop & Classic Rap",
        tempoRange = "110 – 130 BPM",
        characteristics = "Autotuned vocal melodies, atmospheric acoustic or electric guitar pads"
    ),

    // 3. Trap & Modern Urban Sub-Genres
    SubGenre(
        name = "Trapsoul",
        category = "Trap & Modern Urban",
        tempoRange = "110 – 130 BPM",
        characteristics = "Blending 808 trap drums and rolling hi-hats with warm R&B chord progressions"
    ),
    SubGenre(
        name = "Dark Trap",
        category = "Trap & Modern Urban",
        tempoRange = "120 – 140 BPM",
        characteristics = "Heavy distorted 808s, minor key synths, rapid hi-hat rolls, aggressive snare hits"
    ),
    SubGenre(
        name = "Drill (UK / Brooklyn)",
        category = "Trap & Modern Urban",
        tempoRange = "140 – 145 BPM",
        characteristics = "Sliding 808 basslines, erratic snare placement, dark strings/brass samples"
    ),
    SubGenre(
        name = "Phonk / Memphis Rap",
        category = "Trap & Modern Urban",
        tempoRange = "120 – 160 BPM",
        characteristics = "Lo-fi tape distortion, cowbell melodies, heavy 808 bass, chopped-and-screwed vocals"
    ),
    SubGenre(
        name = "Chopper / Fast-Flow Trap",
        category = "Trap & Modern Urban",
        tempoRange = "140 – 160+ BPM",
        characteristics = "High-tempo, intense percussion, rapid double-time vocal cadences"
    )
)

// Legacy alias for backwards compatibility
val SUB_GENRES = MASTER_SUB_GENRES

@Composable
fun GenreSelection(
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    recentSubGenres: List<RecentSubGenre> = emptyList(),
    onGenreSelectedWithDetails: ((String, String, String) -> Unit)? = null
) {
    val categories = MASTER_SUB_GENRES.map { it.category }.distinct()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("genre_selection_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. RECENTLY VIEWED SECTION (LAST 3 USING ROOM PERSISTENCE) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recently_viewed_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MelodiaTheme.Accent)
                        )
                        Text(
                            text = stringResource(R.string.genre_recent_header),
                            fontFamily = NothingDotMatrixFontFamily,
                            fontSize = 13.sp,
                            color = MelodiaTheme.Accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingDarkGrey)
                            .border(1.dp, NothingBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.genre_room_db_badge),
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 9.sp,
                            color = NothingGrey,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (recentSubGenres.isEmpty()) {
                    MelodiaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_recently_viewed")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.genre_no_recent),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MelodiaTheme.TextMain
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.genre_recent_help),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 10.sp,
                                color = MelodiaTheme.TextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    // Display up to 3 recently viewed items
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSubGenres.take(3).forEachIndexed { index, recent ->
                            val isSelected = selectedGenre.equals(recent.name, ignoreCase = true)
                            RecentlyViewedSubGenreCard(
                                index = index + 1,
                                recent = recent,
                                isSelected = isSelected,
                                onClick = {
                                    if (onGenreSelectedWithDetails != null) {
                                        onGenreSelectedWithDetails(recent.name, recent.category, recent.tempoRange)
                                    } else {
                                        onGenreSelected(recent.name)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Decorative divider separating Recently Viewed and Master Catalogue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(NothingBorder)
                    )
                    Text(
                        text = stringResource(R.string.genre_catalogue_header),
                        fontFamily = NothingDotMatrixFontFamily,
                        fontSize = 11.sp,
                        color = NothingGrey,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(NothingBorder)
                    )
                }
            }
        }

        // --- 2. CATEGORIZED MASTER CATALOGUE SECTION ---
        categories.forEach { category ->
            item {
                Text(
                    text = category.uppercase(),
                    fontFamily = NothingDotMatrixFontFamily,
                    fontSize = 13.sp,
                    color = MelodiaTheme.Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    letterSpacing = 1.sp
                )
            }

            val subGenres = MASTER_SUB_GENRES.filter { it.category == category }
            val rows = subGenres.chunked(2)

            items(rows.size) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows[rowIndex].forEach { subGenre ->
                        MelodiaSubGenreItem(
                            subGenre = subGenre,
                            isSelected = selectedGenre.equals(subGenre.name, ignoreCase = true),
                            onClick = {
                                if (onGenreSelectedWithDetails != null) {
                                    onGenreSelectedWithDetails(subGenre.name, subGenre.category, subGenre.tempoRange)
                                } else {
                                    onGenreSelected(subGenre.name)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rows[rowIndex].size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecentlyViewedSubGenreCard(
    index: Int,
    recent: RecentSubGenre,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MelodiaTheme.Accent else NothingBorder
    val bgColor = if (isSelected) NothingCardSurface else NothingDarkSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("recent_subgenre_item_$index")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.genre_rec_badge, index),
                        fontFamily = NothingDotMatrixFontFamily,
                        fontSize = 10.sp,
                        color = MelodiaTheme.Accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recent.category.uppercase(),
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 9.sp,
                        color = MelodiaTheme.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recent.name,
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MelodiaTheme.Accent else MelodiaTheme.TextMain
                )
                if (recent.tempoRange.isNotBlank()) {
                    Text(
                        text = recent.tempoRange,
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 10.sp,
                        color = NothingGrey
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MelodiaTheme.Accent)
                )
            }
        }
    }
}

@Composable
fun MelodiaSubGenreItem(
    subGenre: SubGenre,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MelodiaTheme.Accent else NothingBorder
    val bgColor = if (isSelected) NothingCardSurface else NothingDarkSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("subgenre_item_${subGenre.name.replace(" ", "_")}"),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = subGenre.name.uppercase(),
                fontFamily = SpaceMonoFontFamily,
                fontSize = 11.sp,
                color = if (isSelected) MelodiaTheme.Accent else MelodiaTheme.TextMain,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
            if (subGenre.tempoRange.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subGenre.tempoRange,
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 9.sp,
                    color = if (isSelected) MelodiaTheme.Accent else NothingGrey
                )
            }
        }
    }
}

// Backwards-compatible overload for string name
@Composable
fun MelodiaSubGenreItem(
    subGenre: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matching = MASTER_SUB_GENRES.find { it.name.equals(subGenre, ignoreCase = true) }
        ?: SubGenre(subGenre, "Other")
    MelodiaSubGenreItem(
        subGenre = matching,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    )
}
