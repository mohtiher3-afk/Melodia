package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GenreSelection
import com.example.ui.theme.NothingBlack
import com.example.ui.viewmodel.MelodiaViewModel

/**
 * Standalone Genre Selection screen displaying categorized master subgenres
 * and the 'Recently Viewed' section at the top backed by Room database persistence.
 */
@Composable
fun GenreSelectionScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.studioState.collectAsStateWithLifecycle()
    val recentSubGenres by viewModel.recentSubGenres.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
    ) {
        GenreSelection(
            selectedGenre = state.selectedGenre,
            onGenreSelected = { name ->
                viewModel.recordSubGenreSelection(name)
            },
            recentSubGenres = recentSubGenres,
            onGenreSelectedWithDetails = { name, category, tempo ->
                viewModel.recordSubGenreSelection(name, category, tempo)
            }
        )
    }
}
