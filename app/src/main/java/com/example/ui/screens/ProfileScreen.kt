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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AchievementEntity
import com.example.game.GameViewModel
import com.example.ui.theme.AuroraGold
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.CrystalFrostBevel
import com.example.ui.theme.FrostBaseBackground
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialDeepNavy
import com.example.ui.theme.GlacialSlate
import com.example.ui.theme.SuccessThaw

@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.progressFlow.collectAsState()
    val achievements by viewModel.achievementsFlow.collectAsState()

    val completedLevels = ((progress?.currentLevel ?: 1) - 1).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FrostBaseBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("profile_screen")
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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
                    text = "Player Profile",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = GlacialDeepNavy
                )
            }

            // Profile Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, CrystalFrostBevel, RoundedCornerShape(26.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GlacialBlue,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Ice Architect",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = GlacialDeepNavy
                        )
                        Text(
                            text = "Current Level ${progress?.currentLevel ?: 1}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = GlacialBlue
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = GlacialBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${progress?.crystals ?: 200}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GlacialDeepNavy
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = AuroraGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${progress?.coins ?: 500}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GlacialDeepNavy
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Matrix
            Text(
                text = "Performance Statistics",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GlacialDeepNavy
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(
                    label = "Levels Cleared",
                    value = "$completedLevels",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Perfect Clears",
                    value = "${progress?.perfectLevelsCount ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Daily Streak",
                    value = "${progress?.streak ?: 1}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Achievements Header
            Text(
                text = "Trophies & Achievements",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GlacialDeepNavy
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Achievements List
        items(achievements) { ach ->
            AchievementCardItem(ach)
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.border(1.dp, CrystalFrostBevel, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = GlacialBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = GlacialSlate
            )
        }
    }
}

@Composable
private fun AchievementCardItem(ach: AchievementEntity) {
    val progressRatio = (ach.currentProgress.toFloat() / ach.targetProgress.coerceAtLeast(1))
        .coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CrystalFrostBevel, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (ach.isUnlocked) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (ach.isUnlocked) AuroraGold else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ach.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GlacialDeepNavy
                    )
                    Text(
                        text = "+${ach.rewardCrystals} 💎",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = GlacialBlue
                    )
                }

                Text(
                    text = ach.description,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = GlacialSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (ach.isUnlocked) SuccessThaw else GlacialBlue,
                    trackColor = Color(0xFFF1F5F9)
                )
            }
        }
    }
}
