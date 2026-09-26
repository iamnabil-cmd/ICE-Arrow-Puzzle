package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.GameViewModel
import com.example.ui.components.BoosterBar
import com.example.ui.components.HeaderBar
import com.example.ui.components.IceBoardView
import com.example.ui.components.IceShatterOverlay
import com.example.ui.components.OutOfLivesDialog
import com.example.ui.components.PauseDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.FrostBaseBackground

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val levelState by viewModel.currentLevelState.collectAsState()
    val progress by viewModel.progressFlow.collectAsState()
    val shards by viewModel.shards.collectAsState()

    val curState = levelState ?: return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FrostBaseBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gameplay_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top HUD Navigation (No Replay, No Pause in top bar)
            HeaderBar(
                levelState = curState,
                onBackTapped = { viewModel.onMainMenuTapped() },
                onSettingsTapped = { viewModel.toggleSettingsDialog(true) }
            )

            // Main Ice Puzzle Board (centered, mobile portrait optimized)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IceBoardView(
                    levelState = curState,
                    onArrowTapped = { arrowId -> viewModel.onArrowTapped(arrowId) },
                    onArrowExitCompleted = { arrowId -> viewModel.onArrowExitCompleted(arrowId) },
                    modifier = Modifier.fillMaxSize()
                )

                // Shatter Animation overlay
                if (curState.isShattering) {
                    IceShatterOverlay(shards = shards)
                }
            }

            // Bottom Bar: Hint Button (2 free default) + Grid Alignment Button ([#])
            BoosterBar(
                hintCount = progress?.hintCount ?: 2,
                isGridActive = curState.isGridActive,
                onHintTapped = { viewModel.onHintTapped() },
                onGridTapped = { viewModel.toggleGrid() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // Out of Lives Dialog
        if (curState.isOutOfLives) {
            OutOfLivesDialog(
                onGetFreeLives = { viewModel.refillLivesAndContinue() },
                onRestart = { viewModel.onReplayLevelTapped() }
            )
        }

        // Pause Modal
        if (curState.isPaused) {
            PauseDialog(
                onResume = { viewModel.togglePause() },
                onRestart = {
                    viewModel.togglePause()
                    viewModel.onReplayLevelTapped()
                },
                onQuit = {
                    viewModel.togglePause()
                    viewModel.onMainMenuTapped()
                },
                onDismiss = { viewModel.togglePause() }
            )
        }

        // Settings Modal
        if (curState.showSettingsDialog) {
            SettingsDialog(
                soundEnabled = progress?.soundEnabled ?: true,
                musicEnabled = progress?.musicEnabled ?: true,
                hapticsEnabled = progress?.hapticsEnabled ?: true,
                onToggleSound = {
                    viewModel.updateSettings(
                        sound = it,
                        music = progress?.musicEnabled ?: true,
                        haptics = progress?.hapticsEnabled ?: true
                    )
                },
                onToggleMusic = {
                    viewModel.updateSettings(
                        sound = progress?.soundEnabled ?: true,
                        music = it,
                        haptics = progress?.hapticsEnabled ?: true
                    )
                },
                onToggleHaptics = {
                    viewModel.updateSettings(
                        sound = progress?.soundEnabled ?: true,
                        music = progress?.musicEnabled ?: true,
                        haptics = it
                    )
                },
                onDismiss = { viewModel.toggleSettingsDialog(false) }
            )
        }
    }
}
