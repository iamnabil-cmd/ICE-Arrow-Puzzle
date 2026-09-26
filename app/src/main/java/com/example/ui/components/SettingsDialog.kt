package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CrystalFrostBevel
import com.example.ui.theme.GlacialBlue
import com.example.ui.theme.GlacialDeepNavy
import com.example.ui.theme.GlacialSlate

@Composable
fun SettingsDialog(
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    hapticsEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleMusic: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, CrystalFrostBevel, RoundedCornerShape(26.dp))
                .testTag("settings_dialog")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = GlacialDeepNavy
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GlacialSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingRow(
                    label = "Sound Effects",
                    subtitle = "Crystalline clicks and ice cracks",
                    icon = Icons.Default.GraphicEq,
                    checked = soundEnabled,
                    onCheckedChange = onToggleSound,
                    testTag = "switch_sound"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingRow(
                    label = "Music & Ambient",
                    subtitle = "Serene sub-zero atmosphere",
                    icon = Icons.Default.MusicNote,
                    checked = musicEnabled,
                    onCheckedChange = onToggleMusic,
                    testTag = "switch_music"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingRow(
                    label = "Haptic Feedback",
                    subtitle = "Tactile impact on arrow release",
                    icon = Icons.Default.Vibration,
                    checked = hapticsEnabled,
                    onCheckedChange = onToggleHaptics,
                    testTag = "switch_haptics"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Developer credits badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ICE ARROW PUZZLE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = GlacialDeepNavy
                        )
                        Text(
                            text = "Developed by YusrLab",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = GlacialSlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F9FF),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GlacialBlue,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GlacialDeepNavy
                )
                Text(
                    text = subtitle,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = GlacialSlate
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GlacialBlue
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
