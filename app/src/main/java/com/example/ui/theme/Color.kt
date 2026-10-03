package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Core Accents
val WarXCyan = Color(0xFF00F0FF)
val WarXCyanLight = Color(0xFF0284C7)
val WarXCyanGlow = Color(0x6600F0FF)

val WarXViolet = Color(0xFF8A2BE2)
val WarXVioletLight = Color(0xFF7C3AED)
val WarXVioletGlow = Color(0x668A2BE2)

val WarXEmerald = Color(0xFF10B981)
val WarXAmber = Color(0xFFF59E0B)
val WarXRose = Color(0xFFEF4444)

// Dark Theme Surfaces
val DarkBgStart = Color(0xFF060913)
val DarkBgMid = Color(0xFF0B1021)
val DarkBgEnd = Color(0xFF0F172A)
val DarkGlassSurface = Color(0x331E293B)
val DarkGlassCard = Color(0x24FFFFFF)
val DarkGlassBorder = Color(0x3300F0FF)
val DarkGlassBorderSubtle = Color(0x1FFFFFFF)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF94A3B8)

// Light Theme Surfaces (Arctic Frosted Glass)
val LightBgStart = Color(0xFFF0F4F8)
val LightBgMid = Color(0xFFE2E8F0)
val LightBgEnd = Color(0xFFE8EEF5)
val LightGlassSurface = Color(0xE6FFFFFF)
val LightGlassCard = Color(0xF2FFFFFF)
val LightGlassBorder = Color(0x400284C7)
val LightGlassBorderSubtle = Color(0x260F172A)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF334155)
val LightTextMuted = Color(0xFF64748B)

data class WarXColors(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val glassSurface: Color,
    val glassCard: Color,
    val glassCardHover: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentViolet: Color,
    val accentGradient: Brush,
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusError: Color,
    val statusInfo: Color,
    val ambientGlow: Color,
    val buttonTextOnAccent: Color
)

val DarkWarXColors = WarXColors(
    isDark = true,
    backgroundBrush = Brush.verticalGradient(
        listOf(DarkBgStart, DarkBgMid, DarkBgEnd)
    ),
    glassSurface = DarkGlassSurface,
    glassCard = DarkGlassCard,
    glassCardHover = Color(0x3DFFFFFF),
    glassBorder = DarkGlassBorder,
    glassBorderSubtle = DarkGlassBorderSubtle,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accentCyan = WarXCyan,
    accentViolet = WarXViolet,
    accentGradient = Brush.horizontalGradient(listOf(WarXCyan, WarXViolet)),
    statusSuccess = WarXEmerald,
    statusWarning = WarXAmber,
    statusError = WarXRose,
    statusInfo = Color(0xFF38BDF8),
    ambientGlow = WarXCyanGlow,
    buttonTextOnAccent = Color(0xFF070A14)
)

val LightWarXColors = WarXColors(
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        listOf(LightBgStart, LightBgMid, LightBgEnd)
    ),
    glassSurface = LightGlassSurface,
    glassCard = LightGlassCard,
    glassCardHover = Color(0xFFFFFFFF),
    glassBorder = LightGlassBorder,
    glassBorderSubtle = LightGlassBorderSubtle,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accentCyan = WarXCyanLight,
    accentViolet = WarXVioletLight,
    accentGradient = Brush.horizontalGradient(listOf(WarXCyanLight, WarXVioletLight)),
    statusSuccess = Color(0xFF059669),
    statusWarning = Color(0xFFD97706),
    statusError = Color(0xFFDC2626),
    statusInfo = Color(0xFF0284C7),
    ambientGlow = Color(0x330284C7),
    buttonTextOnAccent = Color.White
)
