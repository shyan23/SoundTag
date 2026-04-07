package com.soundtag.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SoundTagColorScheme = darkColorScheme(
    primary = SoundTagGreen,
    onPrimary = SoundTagBackground,
    secondary = SoundTagGreen,
    onSecondary = SoundTagBackground,
    background = SoundTagBackground,
    onBackground = SoundTagTextPrimary,
    surface = SoundTagSurface,
    onSurface = SoundTagTextPrimary,
    surfaceVariant = SoundTagSurfaceVariant,
    onSurfaceVariant = SoundTagTextSecondary,
    error = SoundTagError,
    onError = SoundTagTextPrimary,
    outline = SoundTagBorder,
    outlineVariant = SoundTagBorder,
)

@Composable
fun SoundTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoundTagColorScheme,
        typography = Typography,
        content = content
    )
}
