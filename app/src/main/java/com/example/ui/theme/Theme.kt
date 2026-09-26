package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlacialColorScheme = lightColorScheme(
    primary = GlacialBlue,
    onPrimary = Color.White,
    primaryContainer = CrystalCyan,
    onPrimaryContainer = GlacialDeepNavy,
    secondary = GlacialBlueLight,
    onSecondary = Color.White,
    secondaryContainer = CrystalCyanLight,
    onSecondaryContainer = GlacialSlate,
    tertiary = AuroraGold,
    onTertiary = GlacialDeepNavy,
    background = FrostBaseBackground,
    onBackground = GlacialDeepNavy,
    surface = FrostSurfaceWhite,
    onSurface = GlacialDeepNavy,
    surfaceVariant = CrystalCyanLight,
    onSurfaceVariant = GlacialSubtleSlate,
    outline = FrostBoardOutline,
    error = ErrorFracture,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GlacialColorScheme,
        typography = AppTypography,
        content = content
    )
}
