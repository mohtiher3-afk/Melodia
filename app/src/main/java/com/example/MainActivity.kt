package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FloatingPlayerBar
import com.example.ui.nothing.NtAppShell
import com.example.ui.nothing.NtDestination
import com.example.ui.nothing.ProjectStep
import com.example.ui.nothing.ntStepLabel
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.VisualsScreen
import com.example.ui.theme.MelodiaTheme
import com.example.ui.viewmodel.MelodiaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelodiaTheme {
                val viewModel: MelodiaViewModel = viewModel()
                var destination by rememberSaveable { mutableStateOf(NtDestination.PROJECT) }
                var projectStep by rememberSaveable { mutableStateOf(ProjectStep.STUDIO) }
                val saveableStateHolder = rememberSaveableStateHolder()

                BackHandler(enabled = destination != NtDestination.PROJECT || projectStep != ProjectStep.STUDIO) {
                    // Preserve the existing behavior: Back returns to the Studio home.
                    destination = NtDestination.PROJECT
                    projectStep = ProjectStep.STUDIO
                }

                val topLabel = when (destination) {
                    NtDestination.PROJECT -> ntStepLabel(projectStep.index)
                    NtDestination.LIBRARY -> androidx.compose.ui.res.stringResource(R.string.nt_nav_library)
                    NtDestination.SETTINGS -> androidx.compose.ui.res.stringResource(R.string.nt_nav_settings)
                }
                val routeKey = when (destination) {
                    NtDestination.PROJECT -> "project:${projectStep.name}"
                    NtDestination.LIBRARY -> "library"
                    NtDestination.SETTINGS -> "settings"
                }

                NtAppShell(
                    selectedDestination = destination.navigationIndex,
                    onSelectDestination = { destination = NtDestination.fromIndex(it) },
                    topLabel = topLabel,
                    stepIndex = projectStep.index.takeIf { destination == NtDestination.PROJECT },
                    onSelectStep = { projectStep = ProjectStep.fromIndex(it) },
                    scrollContent = false,
                    modifier = Modifier.fillMaxSize(),
                    bottomExtra = {
                        FloatingPlayerBar(audioPlayerManager = viewModel.audioPlayerManager)
                    }
                ) {
                    saveableStateHolder.SaveableStateProvider(routeKey) {
                        when (destination) {
                            NtDestination.PROJECT -> when (projectStep) {
                                ProjectStep.STUDIO -> StudioScreen(viewModel = viewModel)
                                ProjectStep.LYRICS -> LyricsScreen(viewModel = viewModel)
                                ProjectStep.VISUALS -> VisualsScreen(viewModel = viewModel)
                                ProjectStep.SYNC -> SyncScreen(viewModel = viewModel)
                                ProjectStep.EXPORT -> ExportScreen(viewModel = viewModel)
                            }
                            NtDestination.LIBRARY -> LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = {
                                    destination = NtDestination.PROJECT
                                    projectStep = ProjectStep.STUDIO
                                }
                            )
                            NtDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
