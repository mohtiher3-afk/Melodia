package com.example

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.IslandController
import com.example.ui.IslandStatus
import com.example.ui.MelodiaTaskIsland
import com.example.ui.MelodiaSplash
import com.example.ui.components.MelodiaMiniPlayer
import com.example.ui.nothing.NtAppShell
import com.example.ui.screens.PlayerScreen
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
import com.example.domain.AudioPlayerManager
import kotlinx.coroutines.launch

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

                MelodiaRoot(
                    viewModel = viewModel,
                    destination = destination,
                    projectStep = projectStep,
                    saveableStateHolder = saveableStateHolder,
                    topLabel = topLabel,
                    routeKey = routeKey,
                    onDestinationChange = { destination = it },
                    onStepChange = { projectStep = it }
                )
            }
        }
    }
}

@Composable
fun MelodiaRoot(
    viewModel: MelodiaViewModel,
    destination: NtDestination,
    projectStep: ProjectStep,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    topLabel: String,
    routeKey: String,
    onDestinationChange: (NtDestination) -> Unit,
    onStepChange: (ProjectStep) -> Unit
) {
    val animationsEnabled = Settings.Global.getInt(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1
    ) > 0

    var ready by rememberSaveable { mutableStateOf(!animationsEnabled) }

    if (!ready) {
        MelodiaSplash(
            onComplete = {
                if (animationsEnabled) ready = true
            }
        )
    } else {
        AppRoot(
            viewModel = viewModel,
            destination = destination,
            projectStep = projectStep,
            saveableStateHolder = saveableStateHolder,
            topLabel = topLabel,
            routeKey = routeKey,
            onDestinationChange = onDestinationChange,
            onStepChange = onStepChange
        )
    }
}

@Composable
fun AppRoot(
    viewModel: MelodiaViewModel,
    destination: NtDestination,
    projectStep: ProjectStep,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    topLabel: String,
    routeKey: String,
    onDestinationChange: (NtDestination) -> Unit,
    onStepChange: (ProjectStep) -> Unit
) {
    val audioPlayerManager = viewModel.audioPlayerManager
    val context = LocalContext.current

    // Observe playback state and update island
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val currentTitle by audioPlayerManager.currentTitle.collectAsState()
    val currentUrl by audioPlayerManager.currentUrl.collectAsState()

    androidx.compose.runtime.LaunchedEffect(isPlaying, currentUrl, currentTitle) {
        if (isPlaying && currentUrl != null) {
            IslandController.showPlayback(currentTitle)
        } else if (!isPlaying) {
            // Only hide if not running a task
            val islandState = IslandController.state.value
            if (islandState.status == IslandStatus.Playing) {
                IslandController.hidePlayback()
            }
        }
    }

    // Full player dialog state
    var showFullPlayer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        NtAppShell(
            selectedDestination = destination.navigationIndex,
            onSelectDestination = { idx -> onDestinationChange(NtDestination.fromIndex(idx)) },
            topLabel = topLabel,
            stepIndex = projectStep.index.takeIf { destination == NtDestination.PROJECT },
            onSelectStep = { idx -> onStepChange(ProjectStep.fromIndex(idx)) },
            scrollContent = false,
            modifier = Modifier.fillMaxSize(),
            bottomExtra = {
                MelodiaMiniPlayer(
                    audioPlayerManager = viewModel.audioPlayerManager,
                    onOpenFullPlayer = { showFullPlayer = true }
                )
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
                            onDestinationChange(NtDestination.PROJECT)
                            onStepChange(ProjectStep.STUDIO)
                        }
                    )
                    NtDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Full player dialog
        if (showFullPlayer) {
            PlayerScreen(
                audioPlayerManager = audioPlayerManager,
                onDismiss = { showFullPlayer = false }
            )
        }

        // Top-center island overlay. Box-anchored so it never pushes content down.
        val islandUi by IslandController.state.collectAsState()
        if (islandUi.status != IslandStatus.Idle) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("melodia_island_overlay"),
                contentAlignment = Alignment.TopCenter
            ) {
                MelodiaTaskIsland(
                    uiState = islandUi,
                    onToggle = { IslandController.toggle() }
                )
            }
        }
    }
}