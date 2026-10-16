package com.example.cvlab.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary          = Mint,
    onPrimary        = Ink,
    primaryContainer = MintDim,
    onPrimaryContainer = Ink,
    secondary        = Sky,
    onSecondary      = Ink,
    secondaryContainer = SkyDim,
    onSecondaryContainer = Color.White,
    tertiary         = Gold,
    onTertiary       = Ink,
    tertiaryContainer = GoldDim,
    onTertiaryContainer = Color.White,
    error            = Coral,
    onError          = Ink,
    errorContainer   = CoralDim,
    onErrorContainer = Color.White,
    background       = Ink,
    onBackground     = Color.White,
    surface          = InkSurface,
    onSurface        = Color.White,
    surfaceVariant   = Panel,
    onSurfaceVariant = Color(0xFFADBDD4),
    outline          = Border,
    outlineVariant   = Color(0xFF1E2D47),
    scrim            = Color(0x99000000),
)

val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF007B5E),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFA0F5DC),
    onPrimaryContainer = Color(0xFF00382C),
    secondary        = Color(0xFF1565C0),
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFBBD8FF),
    onSecondaryContainer = Color(0xFF001E3D),
    tertiary         = Color(0xFFB06000),
    onTertiary       = Color.White,
    tertiaryContainer = Color(0xFFFFDDB5),
    onTertiaryContainer = Color(0xFF371D00),
    error            = Color(0xFFB00020),
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF1A1A2E),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A2E),
    surfaceVariant   = Color(0xFFE8EFF5),
    onSurfaceVariant = Color(0xFF3A4D5E),
    outline          = Color(0xFF90A4AE),
)
