package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.AppThemeMode

val LocalWarXColors = staticCompositionLocalOf { DarkWarXColors }

object WarXTheme {
    val colors: WarXColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWarXColors.current
}

private val M3DarkColorScheme = darkColorScheme(
    primary = WarXCyan,
    onPrimary = Color(0xFF070A14),
    primaryContainer = Color(0xFF004D54),
    onPrimaryContainer = Color(0xFF70F3FF),
    secondary = WarXViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFDDD6FE),
    background = DarkBgStart,
    onBackground = DarkTextPrimary,
    surface = DarkBgMid,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgEnd,
    onSurfaceVariant = DarkTextSecondary
)

private val M3LightColorScheme = lightColorScheme(
    primary = WarXCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = WarXVioletLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    background = LightBgStart,
    onBackground = LightTextPrimary,
    surface = LightGlassCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBgMid,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun WarXTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val customColors = if (isDark) DarkWarXColors else LightWarXColors
    val m3Colors = if (isDark) M3DarkColorScheme else M3LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Ensure edge to edge transparent bars
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalWarXColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = Typography,
            content = content
        )
    }
}
