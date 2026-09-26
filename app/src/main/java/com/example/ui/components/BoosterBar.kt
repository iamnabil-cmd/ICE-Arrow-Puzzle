package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuroraGold
import com.example.ui.theme.CrystalFrostBevel
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialDeepNavy
import com.example.ui.theme.GlacialSlate

@Composable
fun BoosterBar(
    hintCount: Int,
    isGridActive: Boolean,
    onHintTapped: () -> Unit,
    onGridTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Hint Button (Free 2 hints default)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF2FFFFFF),
            shadowElevation = 8.dp,
            modifier = Modifier
                .border(1.5.dp, CrystalFrostBevel, RoundedCornerShape(24.dp))
                .clickable(onClick = onHintTapped)
                .testTag("booster_hint")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Hint",
                                tint = AuroraGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Count Badge: goes from 2 -> 1 -> Ad Icon when exhausted, perfectly centered
                    Surface(
                        shape = CircleShape,
                        color = if (hintCount > 0) Color(0xFFD97706) else Color(0xFF2563EB),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hintCount > 0) {
                                Text(
                                    text = "$hintCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeight = 11.sp
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Ad for Hint",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Hint",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GlacialDeepNavy
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Grid Alignment Guide Button (Matching Grid.mp4 [#] toggle)
        Surface(
            shape = CircleShape,
            color = if (isGridActive) GlacialBlue else Color(0xF2FFFFFF),
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(54.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isGridActive) GlacialBlue else CrystalFrostBevel,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onGridTapped)
                .testTag("booster_grid")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid Alignment Guides",
                    tint = if (isGridActive) Color.White else GlacialSlate,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
