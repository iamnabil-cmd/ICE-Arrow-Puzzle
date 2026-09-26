package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.GlacialDeepNavy
import com.example.ui.theme.GlacialSlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyChallengeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progressFlow.collectAsState()
    val streak = progress?.streak ?: 1
    val todayDateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FrostBaseBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .testTag("daily_challenge_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onMainMenuTapped() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GlacialSlate
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Challenge",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = GlacialDeepNavy
                )
            }

            // Hero Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CrystalFrostBevel, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = GlacialBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = todayDateStr,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = GlacialDeepNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Glacial Frost Expedition",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = GlacialSlate
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats in card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$streak Day Streak",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GlacialDeepNavy
                                )
                            }
                            Text(
                                text = "Keep it active!",
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = GlacialSlate
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = GlacialBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+300 Crystals",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GlacialDeepNavy
                                )
                            }
                            Text(
                                text = "Double Reward",
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = GlacialSlate
                            )
                        }
                    }
                }
            }

            // Play Daily Button
            Button(
                onClick = {
                    // Load special daily puzzle (e.g. level 12)
                    viewModel.loadLevel(12)
                    viewModel.onBackToGameplay()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("play_daily_button"),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlacialBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY TODAY'S PUZZLE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}
