package com.local.snldemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AmoledBackground = Color(0xFF0B0B0F)
val AmoledSurface = Color(0xFF16161E)
val AmoledSurfaceVariant = Color(0xFF20202C)
val AmoledPrimary = Color(0xFF4E95FF)
val AmoledSecondary = Color(0xFF00D2D3)
val AmoledError = Color(0xFFFF5252)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    secondary = AmoledSecondary,
    tertiary = Color(0xFF2ED573),
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    error = AmoledError,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),
    onBackground = Color(0xFFF1F2F6),
    onSurface = Color(0xFFF1F2F6),
    onSurfaceVariant = Color(0xFFA4B0BE)
)

@Composable
fun SNLDemoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledDarkColorScheme,
        content = content
    )
}
