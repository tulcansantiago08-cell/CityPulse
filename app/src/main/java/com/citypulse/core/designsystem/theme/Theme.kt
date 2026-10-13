package com.citypulse.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CityPulseColorScheme = darkColorScheme(
    primary = CpColors.Magenta,
    onPrimary = CpColors.TextPrimary,
    secondary = CpColors.Cyan,
    onSecondary = CpColors.Background,
    tertiary = CpColors.Live,
    onTertiary = CpColors.TextPrimary,
    background = CpColors.Background,
    onBackground = CpColors.TextPrimary,
    surface = CpColors.Background,
    onSurface = CpColors.TextPrimary,
    surfaceVariant = CpColors.Surface,
    onSurfaceVariant = CpColors.TextSecondary,
    surfaceContainer = CpColors.Surface,
    outline = CpColors.GlassBorder,
    outlineVariant = CpColors.GlassBorderStrong,
)

/** Tema de CityPulse. La app es solo en modo oscuro, como el prototipo. */
@Composable
fun CityPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CityPulseColorScheme,
        typography = CityPulseTypography,
        content = content,
    )
}
