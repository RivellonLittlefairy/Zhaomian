package com.example.passerwatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val DayColorScheme = lightColorScheme(
    primary = Color(0xFF146B50), onPrimary = Color.White,
    primaryContainer = Color(0xFFC2EDDC), onPrimaryContainer = Color(0xFF103C2D),
    secondary = Color(0xFF356779), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EBF3), onSecondaryContainer = Color(0xFF153E4B),
    tertiary = Color(0xFF85551D), onTertiary = Color.White,
    background = Color(0xFFF4F7F4), onBackground = Color(0xFF1A2922),
    surface = Color(0xFFF4F7F4), onSurface = Color(0xFF1A2922),
    surfaceVariant = Color(0xFFE2EAE4), onSurfaceVariant = Color(0xFF52645A),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEDF3EE),
    surfaceContainer = Color(0xFFE7EFE9), surfaceContainerHigh = Color(0xFFDEE9E1),
    surfaceContainerHighest = Color(0xFFD5E3D9), outline = Color(0xFF72857A),
    outlineVariant = Color(0xFFCCD9D0), error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

@Composable
fun ZhaomianTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) SentryColorScheme else DayColorScheme,
        typography = Typography, content = content)
}