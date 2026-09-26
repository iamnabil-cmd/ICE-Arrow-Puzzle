package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.ui.theme.AuroraGold
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.CrystalFrostBevel
import com.example.ui.theme.FrostBaseBackground
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialBlueLight
import com.example.ui.theme.GlacialDeepNavy
import com.example.ui.theme.GlacialSlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainHomeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progressFlow.collectAsState()
    val currentLevel = progress?.currentLevel ?: 1
    val todayDateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())

    Scaffold(
        bottomBar = {
            // Bottom Bar: Main, Daily, Me (Image 15)
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, CrystalFrostBevel)
                    .testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Main */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Main") },
                    label = { Text("Main", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GlacialBlue,
                        selectedTextColor = GlacialBlue,
                        indicatorColor = Color(0xFFE0F2FE)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onDailyChallengeTapped() },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Daily") },
                    label = { Text("Daily", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = GlacialSlate,
                        unselectedTextColor = GlacialSlate
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.onProfileTapped() },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Me") },
                    label = { Text("Me", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = GlacialSlate,
                        unselectedTextColor = GlacialSlate
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(FrostBaseBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Card: Daily Challenge (Image 15)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(GlacialBlueLight, GlacialBlue)
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Calendar / Daily icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = AuroraGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "DAILY CHALLENGE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = todayDateStr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // "Continue" pill button
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { viewModel.onDailyChallengeTapped() }
                            ) {
                                Text(
                                    text = "Continue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Middle: ICE Arrow Puzzle Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ICE Arrow Puzzle",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = GlacialDeepNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Slide the unblocked arrows to shatter the ice",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = GlacialSlate
                    )
                }

                // Bottom Buttons: Continue Game & Restart Game
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Continue Game (Level X)
                    Button(
                        onClick = {
                            viewModel.loadLevel(currentLevel)
                            viewModel.onBackToGameplay()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("continue_game_button"),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlacialBlue)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Continue Game",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Level $currentLevel",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Restart Game
                    OutlinedButton(
                        onClick = {
                            viewModel.loadLevel(currentLevel)
                            viewModel.onBackToGameplay()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("restart_game_menu_button"),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            text = "Restart Game",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GlacialBlue
                        )
                    }
                }
            }
        }
    }
}
