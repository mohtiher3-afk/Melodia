package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MelodiaViewModel

@Composable
fun ExportScreen(
    viewModel: MelodiaViewModel,
    modifier: Modifier = Modifier,
) {
    VideoExporterScreen(viewModel = viewModel, modifier = modifier)
}
