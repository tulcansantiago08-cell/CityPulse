package com.citypulse.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Tokens de color extraídos del prototipo de CityPulse. */
object CpColors {
    val BackgroundDeep = Color(0xFF060509)
    val Background = Color(0xFF0B0A10)
    val Surface = Color(0xFF16141F)
    val SurfaceMap = Color(0xFF100E17)

    val Magenta = Color(0xFFDC21B8)
    val Cyan = Color(0xFF4AC0FC)
    val Live = Color(0xFFFF5A1F)
    val LiveSoft = Color(0xFFFF8A5C)

    val Glass = Color.White.copy(alpha = 0.08f)
    val GlassSubtle = Color.White.copy(alpha = 0.04f)
    val GlassBorder = Color.White.copy(alpha = 0.10f)
    val GlassBorderStrong = Color.White.copy(alpha = 0.14f)

    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.70f)
    val TextTertiary = Color.White.copy(alpha = 0.60f)
    val TextMuted = Color.White.copy(alpha = 0.45f)
}

/** Gradiente de marca en diagonal (135°), usado en anillos, iconos y botones circulares. */
val BrandGradient: Brush = Brush.linearGradient(listOf(CpColors.Magenta, CpColors.Cyan))

/** Gradiente de marca horizontal (90°), usado en botones y textos destacados. */
val BrandGradientHorizontal: Brush = Brush.horizontalGradient(listOf(CpColors.Magenta, CpColors.Cyan))
