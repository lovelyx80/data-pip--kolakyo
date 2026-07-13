/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Spotify Theme Extensions
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Spotify Dark Theme Color Palette
 */
object SpotifyTheme {
    // Primary Colors
    val Primary = Color(0xFF1DB954) // Spotify Green
    val PrimaryDark = Color(0xFF1aa34a)
    val PrimaryLight = Color(0xFF1ed760)

    // Background Colors
    val Background = Color(0xFF121212) // Pure black
    val SurfaceVariant = Color(0xFF1E1E1E) // Dark grey
    val Surface = Color(0xFF282828) // Medium grey
    val SurfaceLight = Color(0xFF404040) // Light grey

    // Text Colors
    val OnSurface = Color(0xFFFFFFFF) // White
    val OnSurfaceVariant = Color(0xFFB3B3B3) // Light grey
    val OnSurfaceWeak = Color(0xFF808080) // Medium grey

    // Semantic Colors
    val Error = Color(0xFFE22134) // Spotify Error Red
    val Success = Primary
    val Warning = Color(0xFFF4A460) // Orange

    // Additional
    val Divider = Color(0xFF282828)
    val Premium = Color(0xFFFFD700) // Gold for premium features
}

/**
 * Apply Spotify theme to existing ColorScheme
 */
fun ColorScheme.applySpotifyTheme(): ColorScheme {
    return this.copy(
        primary = SpotifyTheme.Primary,
        onPrimary = Color.Black,
        primaryContainer = SpotifyTheme.PrimaryDark,
        onPrimaryContainer = Color.White,
        secondary = SpotifyTheme.Surface,
        onSecondary = Color.White,
        tertiary = SpotifyTheme.Primary,
        onTertiary = Color.Black,
        background = SpotifyTheme.Background,
        onBackground = SpotifyTheme.OnSurface,
        surface = SpotifyTheme.Surface,
        onSurface = SpotifyTheme.OnSurface,
        surfaceVariant = SpotifyTheme.SurfaceVariant,
        onSurfaceVariant = SpotifyTheme.OnSurfaceVariant,
        error = SpotifyTheme.Error,
        onError = Color.White,
        outline = SpotifyTheme.SurfaceLight,
        outlineVariant = SpotifyTheme.Surface
    )
}
