package com.citypulse.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CpColors

/**
 * Fondo "glass" del prototipo: blanco translúcido con borde sutil.
 *
 * Compose no ofrece desenfoque del contenido de fondo (backdrop-filter), así que el efecto
 * se aproxima con la transparencia, que sobre el fondo oscuro da un resultado muy similar.
 */
fun Modifier.glass(
    shape: Shape,
    fill: Color = CpColors.Glass,
    borderColor: Color = CpColors.GlassBorder,
): Modifier = this
    .background(fill, shape)
    .border(1.dp, borderColor, shape)

/** Borde con gradiente, como el anillo de las stories y del pin seleccionado. */
fun Modifier.gradientBorder(width: Dp, brush: Brush, shape: Shape): Modifier =
    this.border(width, brush, shape)

/** Resplandor de color bajo un elemento (box-shadow coloreado del prototipo). */
fun Modifier.glow(
    shape: Shape,
    color: Color = CpColors.Magenta,
    elevation: Dp = 16.dp,
): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this.shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)
    } else {
        this
    }

/** Degradado oscuro sobre una imagen para que el texto se lea bien. */
fun scrimBrush(vararg stops: Pair<Float, Float>): Brush =
    Brush.verticalGradient(
        colorStops = stops.map { (offset, alpha) -> offset to CpColors.Background.copy(alpha = alpha) }
            .toTypedArray(),
    )

/** Pinta el contenido (p. ej. un icono blanco) con el gradiente de marca. */
fun Modifier.gradientTint(brush: Brush = BrandGradient): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        }
    }
