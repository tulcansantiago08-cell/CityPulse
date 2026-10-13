package com.citypulse.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.citypulse.R

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun poppins(size: Int, weight: FontWeight, lineHeight: Float = 1.3f, letterSpacing: Float = 0f) =
    TextStyle(
        fontFamily = Poppins,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        letterSpacing = letterSpacing.sp,
    )

val CityPulseTypography = Typography(
    // Títulos grandes en mayúsculas sobre imágenes (onboarding, sorpresa)
    displayLarge = poppins(34, FontWeight.SemiBold, lineHeight = 1.05f, letterSpacing = -0.5f),
    displayMedium = poppins(30, FontWeight.SemiBold, lineHeight = 1.05f),
    // Encabezados de pantalla
    headlineLarge = poppins(26, FontWeight.SemiBold, lineHeight = 1.1f),
    headlineMedium = poppins(24, FontWeight.SemiBold, lineHeight = 1.2f),
    headlineSmall = poppins(22, FontWeight.SemiBold, lineHeight = 1.1f),
    // Títulos de sección y tarjetas
    titleLarge = poppins(20, FontWeight.SemiBold),
    titleMedium = poppins(18, FontWeight.SemiBold),
    titleSmall = poppins(15, FontWeight.SemiBold, lineHeight = 1.2f),
    // Cuerpo
    bodyLarge = poppins(14, FontWeight.Light, lineHeight = 1.6f),
    bodyMedium = poppins(13, FontWeight.Light, lineHeight = 1.5f),
    bodySmall = poppins(12, FontWeight.Light, lineHeight = 1.4f),
    // Etiquetas y botones
    labelLarge = poppins(15, FontWeight.SemiBold),
    labelMedium = poppins(11, FontWeight.Medium, letterSpacing = 0.4f),
    labelSmall = poppins(10, FontWeight.SemiBold, letterSpacing = 0.6f),
)

/** Estilo de las etiquetas en mayúsculas con tracking amplio ("ESTOY AQUÍ AHORA", "CIUDADES POPULARES"). */
val OverlineStyle = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.13.em,
)
