package com.cinesakura.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SakuraPink,
    onPrimary = Color.White,
    primaryContainer = SakuraDark,
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363F),
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = Color(0xFFF43F5E)
)

@Composable
fun CineSakuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
