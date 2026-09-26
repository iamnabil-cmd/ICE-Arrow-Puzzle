package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.game.GameViewModel
import com.example.game.ScreenState
import com.example.ui.screens.DailyChallengeScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LevelCompleteScreen
import com.example.ui.screens.MainHomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.FrostBaseBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FrostBaseBackground
                ) {
                    IceArrowApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun IceArrowApp(viewModel: GameViewModel) {
    val screenState by viewModel.screenState.collectAsState()

    // System back goes to the main menu from every screen; on the main menu it leaves the app as usual
    BackHandler(enabled = screenState != ScreenState.MAIN_MENU && screenState != ScreenState.SPLASH) {
        viewModel.onMainMenuTapped()
    }

    when (screenState) {
        ScreenState.SPLASH -> {
            SplashScreen()
        }
        ScreenState.GAMEPLAY -> {
            GameplayScreen(viewModel = viewModel)
        }
        ScreenState.LEVEL_COMPLETE -> {
            LevelCompleteScreen(viewModel = viewModel)
        }
        ScreenState.MAIN_MENU -> {
            MainHomeScreen(viewModel = viewModel)
        }
        ScreenState.DAILY_CHALLENGE -> {
            DailyChallengeScreen(viewModel = viewModel)
        }
        ScreenState.PROFILE -> {
            ProfileScreen(viewModel = viewModel)
        }
    }
}
