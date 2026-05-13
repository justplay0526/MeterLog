package com.justplay.meterlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFFB45309),
    tertiary = Color(0xFF2563EB),
    background = Color(0xFFE3EEE9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD4E2DC),
    onSurface = Color(0xFF1C1C18),
    onSurfaceVariant = Color(0xFF555048),
    outlineVariant = Color(0xFFB8CCC4),
    error = Color(0xFFB91C1C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF003C38),
    secondary = Color(0xFFFBBF24),
    tertiary = Color(0xFF93C5FD),
    background = Color(0xFF17211D),
    surface = Color(0xFF22302A),
    surfaceVariant = Color(0xFF4A554F),
    onSurface = Color(0xFFE5E5DD),
    onSurfaceVariant = Color(0xFFC8C7BE),
    outlineVariant = Color(0xFF4A554F),
    error = Color(0xFFFCA5A5)
)

@Composable
fun MeterLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
