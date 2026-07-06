package com.tertiaryinfotech.jingangjing.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color tokens ported from the iOS Asset Catalog (warm saffron/bronze on sutra paper).
private val Accent = Color(0xFF9C5628)          // Accent, light
private val AccentDark = Color(0xFFD9A35B)      // Accent, dark
private val Paper = Color(0xFFF7F1E3)           // Background, light
private val PaperDark = Color(0xFF171512)       // Background, dark
private val CardLight = Color(0xFFFFFDF5)       // Card, light
private val CardDark = Color(0xFF26231F)        // Card, dark
private val Ink = Color(0xFF2E261F)             // Ink, light
private val InkDark = Color(0xFFE8E1D4)         // Ink, dark

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E0CB),
    onPrimaryContainer = Color(0xFF46250F),
    secondary = Accent,
    secondaryContainer = Color(0xFFF0E0CB),     // selected nav pill
    onSecondaryContainer = Color(0xFF46250F),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = CardLight,                 // card surface
    onSurfaceVariant = Color(0xFF7A6F63),
    surfaceContainer = CardLight,
    surfaceContainerHigh = Color(0xFFF3EADA),
    outlineVariant = Color(0xFFE2D6C2),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF3E2410),
    primaryContainer = Color(0xFF5C3A20),
    onPrimaryContainer = Color(0xFFF0E0CB),
    secondary = AccentDark,
    secondaryContainer = Color(0xFF5C3A20),     // selected nav pill
    onSecondaryContainer = Color(0xFFF0E0CB),
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = CardDark,                  // card surface
    onSurfaceVariant = Color(0xFFA99C8C),
    surfaceContainer = CardDark,
    surfaceContainerHigh = Color(0xFF2E2A24),
    outlineVariant = Color(0xFF3E382F),
)

@Composable
fun JingangJingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
