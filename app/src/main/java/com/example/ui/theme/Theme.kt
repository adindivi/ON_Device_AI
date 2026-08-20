package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Toss-Inspired Minimal B&W Light Scheme ────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = TossBlack,
    onPrimary          = TossWhite,
    primaryContainer   = TossGray100,
    onPrimaryContainer = TossBlack,
    secondary          = TossGray600,
    onSecondary        = TossWhite,
    secondaryContainer = TossGray100,
    onSecondaryContainer = TossGray600,
    tertiary           = CarTertiary,
    tertiaryContainer  = CarTertiaryContainer,
    onTertiaryContainer = CarTertiary,
    background         = TossWhite,
    onBackground       = TossBlack,
    surface            = TossWhite,
    onSurface          = TossBlack,
    surfaceVariant     = TossGray100,
    onSurfaceVariant   = TossGray600,
    outline            = TossOutline,
    error              = CarError,
    errorContainer     = CarErrorContainer,
    onErrorContainer   = CarOnErrorContainer
)

// ── Dark Scheme (minimal change) ──────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFB4C5FF),
    onPrimary          = Color(0xFF002A78),
    primaryContainer   = CarPrimary,
    onPrimaryContainer = Color.White,
    secondary          = Color(0xFFB9C7E0),
    background         = Color(0xFF121316),
    onBackground       = Color(0xFFE2E2E6),
    surface            = Color(0xFF1A1C1E),
    onSurface          = Color(0xFFE2E2E6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
