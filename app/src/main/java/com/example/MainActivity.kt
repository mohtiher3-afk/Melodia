package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ambientGlow
import com.example.ui.components.FloatingPlayerBar
import com.example.ui.components.MelodiaTab
import com.example.ui.components.NothingBottomBar
import com.example.ui.dotGrid
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.VisualsScreen
import com.example.ui.theme.MelodiaTheme
import com.example.ui.theme.NothingBlack
import com.example.ui.viewmodel.MelodiaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelodiaTheme {
                val viewModel: MelodiaViewModel = viewModel()
                var currentTab by rememberSaveable { mutableStateOf(MelodiaTab.STUDIO) }

                BackHandler(enabled = currentTab != MelodiaTab.STUDIO) {
                    currentTab = MelodiaTab.STUDIO
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NothingBlack),
                    bottomBar = {
                        Column(modifier = Modifier.navigationBarsPadding()) {
                            FloatingPlayerBar(
                                audioPlayerManager = viewModel.audioPlayerManager,
                            )
                            NothingBottomBar(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it },
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(NothingBlack)
                            .ambientGlow()
                            .dotGrid()
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val enterOffset = if (forward) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = tween(220),
                                    initialOffsetX = { (it / 12) * enterOffset }
                                ) + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(180),
                                            targetOffsetX = { (-it / 16) * enterOffset }
                                        ) + fadeOut(animationSpec = tween(120))
                                    )
                            },
                            label = "melodia_tab_content"
                        ) { tab ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (tab) {
                                    MelodiaTab.STUDIO -> StudioScreen(viewModel = viewModel)
                                    MelodiaTab.LYRICS -> LyricsScreen(viewModel = viewModel)
                                    MelodiaTab.LIBRARY -> LibraryScreen(
                                        viewModel = viewModel,
                                        onNavigateToStudio = { currentTab = MelodiaTab.STUDIO }
                                    )
                                    MelodiaTab.VISUALS -> VisualsScreen(viewModel = viewModel)
                                    MelodiaTab.SYNC -> SyncScreen(viewModel = viewModel)
                                    MelodiaTab.EXPORT -> ExportScreen(viewModel = viewModel)
                                    MelodiaTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
