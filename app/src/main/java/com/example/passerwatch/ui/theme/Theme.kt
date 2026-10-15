package com.example.passerwatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SentryColorScheme = darkColorScheme(
    primary = SentryGreen,
    onPrimary = Color(0xFF00210B),
    primaryContainer = SentryGreen,
    onPrimaryContainer = Color(0xFF00210B),
    secondary = TelemetryCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9CF0FF),
    tertiary = SentryAmber,
    onTertiary = Color(0xFF432C00),
    background = SentryBackground,
    onBackground = Color(0xFFE1E3E4),
    surface = SentryBackground,
    onSurface = Color(0xFFE1E3E4),
    surfaceVariant = SentryBorder,
    onSurfaceVariant = SentryMuted,
    surfaceContainerLowest = SentryLowest,
    surfaceContainerLow = SentryPanel,
    surfaceContainer = Color(0xFF1D2021),
    surfaceContainerHigh = Color(0xFF272A2B),
    surfaceContainerHighest = SentryBorder,
    outline = Color(0xFF859585),
    outlineVariant = Color(0xFF3B4A3D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ZhaomianTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep the instrument's status colors consistent across device settings.
    MaterialTheme(colorScheme = SentryColorScheme, typography = Typography, content = content)
}
