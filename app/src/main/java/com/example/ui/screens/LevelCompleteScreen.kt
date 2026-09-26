package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.game.GameViewModel
import com.example.ui.theme.AuroraGold
import com.example.ui.theme.CrystalCyan
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialBlueDark
import com.example.ui.theme.GlacialBlueLight
import com.example.ui.theme.GlacialDeepNavy
import kotlin.random.Random

@Composable
fun LevelCompleteScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val lastLevel by viewModel.lastCompletedLevel.collectAsState()
    val accuracy by viewModel.accuracyPercent.collectAsState()
    val clearTimeMs by viewModel.lastClearTimeMs.collectAsState()
    val mistakes by viewModel.lastMistakes.collectAsState()
    val levelDef = lastLevel ?: return

    // Falling confetti anim
    val confettiAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        confettiAnim.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0077D4), Color(0xFF0052A3), Color(0xFF003875)),
                    radius = 1200f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("level_complete_screen")
    ) {
        // Confetti Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val confettiColors = listOf(
                Color(0xFFFFD166), Color(0xFF06D6A0), Color(0xFF118AB2),
                Color(0xFFEF476F), Color(0xFFF78C6B), Color.White
            )
            val random = Random(42)
            for (i in 0 until 50) {
                val startX = random.nextFloat() * w
                val speed = 0.4f + random.nextFloat() * 0.8f
                val y = ((confettiAnim.value * speed * h * 1.5f) + (i * 25)) % (h + 50)
                val c = confettiColors[i % confettiColors.size]
                val sizeW = 10f + (i % 8)
                val sizeH = 18f + (i % 6)
                drawRect(
                    color = c,
                    topLeft = Offset(startX, y),
                    size = androidx.compose.ui.geometry.Size(sizeW, sizeH)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Cleared Banner
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AuroraGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LEVEL ${levelDef.levelNumber} CLEARED",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "Level Completed!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Aurora Stars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StarBadge(size = 46.dp, isElevated = false)
                    StarBadge(size = 58.dp, isElevated = true)
                    StarBadge(size = 46.dp, isElevated = false)
                }
            }

            // Central Solved Card Tailored to Completed Level (replaces generic 3 lines from Level-clear.jpg)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0F9FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = GlacialBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LEVEL ${levelDef.levelNumber + 1} UNLOCKED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = GlacialBlue
                                )
                            }
                        }

                        Text(
                            text = "${levelDef.arrows.size} ARROWS CLEARED",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = GlacialBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tailored Unlocked Achievement Item
                    val rewardRes = levelDef.rewardDrawableRes ?: com.example.R.drawable.img_reward_silver_coin
                    Box(
                        modifier = Modifier
                            .size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient radial soft glow
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x66FEF08A),
                                            Color(0x33BAE6FD),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        Image(
                            painter = painterResource(id = rewardRes),
                            contentDescription = levelDef.rewardItemName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(86.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unlocked Item Name Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = "✨ ${levelDef.rewardItemName} ✨",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // How the level went: time to clear all arrows, and how many wrong arrows were tapped
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultChip(
                            icon = Icons.Default.Timer,
                            text = formatClearTime(clearTimeMs),
                            background = Color(0xFFEBF5FF),
                            tint = GlacialBlue,
                            modifier = Modifier.testTag("clear_time")
                        )
                        val noMistakes = mistakes == 0
                        ResultChip(
                            icon = if (noMistakes) Icons.Default.Check else Icons.Default.Close,
                            text = if (mistakes == 1) "1 Mistake" else "$mistakes Mistakes",
                            background = if (noMistakes) Color(0xFFE7F8EF) else Color(0xFFFDECEC),
                            tint = if (noMistakes) Color(0xFF15803D) else Color(0xFFDC2626),
                            modifier = Modifier.testTag("mistakes")
                        )
                    }
                }
            }

            // Stat Cards (Crystals & Accuracy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Crystals Card
                StatPillCard(
                    icon = Icons.Default.Diamond,
                    iconBg = CrystalCyan,
                    label = "Crystals",
                    value = "+${levelDef.rewardCrystals}",
                    modifier = Modifier.weight(1f)
                )

                // Accuracy Card
                StatPillCard(
                    icon = Icons.Default.Bolt,
                    iconBg = AuroraGold,
                    label = "Accuracy",
                    value = "$accuracy%",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Volumetric Action Button (Next Game -> Level X+1)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { viewModel.onNextLevelTapped() }
                        .testTag("next_level_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White, Color(0xFFF0F9FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Next Game",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = GlacialBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = GlacialBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Level ${levelDef.levelNumber + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GlacialBlueLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Navigation (Replay & Main)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.onReplayLevelTapped() }
                            .padding(8.dp)
                            .testTag("replay_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Replay",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.onMainMenuTapped() }
                            .padding(8.dp)
                            .testTag("main_menu_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Main",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Main",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarBadge(size: androidx.compose.ui.unit.Dp, isElevated: Boolean) {
    Surface(
        shape = CircleShape,
        color = Color(0x33FFFFFF),
        modifier = Modifier
            .size(size)
            .border(2.dp, AuroraGold, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star",
                tint = AuroraGold,
                modifier = Modifier.size(size * 0.65f)
            )
        }
    }
}

@Composable
private fun StatPillCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x40FFFFFF),
        modifier = modifier
            .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GlacialDeepNavy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ResultChip(
    icon: ImageVector,
    text: String,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = background,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = tint
            )
        }
    }
}

/** 42_000 ms -> "0:42", 125_000 ms -> "2:05", 3_725_000 ms -> "1:02:05". */
internal fun formatClearTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
