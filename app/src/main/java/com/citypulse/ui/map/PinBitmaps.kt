package com.citypulse.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.citypulse.core.designsystem.theme.CpColors
import kotlin.math.max

/** Tipo de anillo del pin: seleccionado (gradiente), en curso (naranja) o normal. */
enum class PinRing { Selected, Live, Normal }

/**
 * Dibuja el pin circular del prototipo: anillo, borde oscuro y la foto del evento
 * (o su degradado de respaldo). El seleccionado lleva además un resplandor magenta.
 */
fun pinBitmap(
    density: Float,
    photo: Bitmap?,
    fallback: List<Color>,
    ring: PinRing,
): Bitmap {
    val sizeDp = if (ring == PinRing.Selected) 54f else 38f
    val glowDp = if (ring == PinRing.Selected) 12f else 2f
    val size = sizeDp * density
    val margin = glowDp * density
    val total = (size + margin * 2).toInt()
    val bitmap = createBitmap(total, total)
    val canvas = Canvas(bitmap)
    val c = total / 2f
    val radius = size / 2f

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        when (ring) {
            PinRing.Selected -> {
                shader = LinearGradient(c - radius, c - radius, c + radius, c + radius, CpColors.Magenta.toArgb(), CpColors.Cyan.toArgb(), Shader.TileMode.CLAMP)
                setShadowLayer(margin * 0.9f, 0f, 0f, CpColors.Magenta.copy(alpha = 0.7f).toArgb())
            }
            PinRing.Live -> color = CpColors.Live.toArgb()
            PinRing.Normal -> color = Color.White.copy(alpha = 0.4f).toArgb()
        }
    }
    canvas.drawCircle(c, c, radius, ringPaint)

    val border = 2f * density
    canvas.drawCircle(c, c, radius - border, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CpColors.Background.toArgb() })

    val inner = radius - border * 2
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    if (photo != null) {
        // Recorte centrado ("center crop") de la foto dentro del círculo.
        val scale = max(inner * 2 / photo.width, inner * 2 / photo.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(c - photo.width * scale / 2, c - photo.height * scale / 2)
        }
        fill.shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
    } else {
        fill.shader = LinearGradient(c - inner, c - inner, c + inner, c + inner, fallback.first().toArgb(), fallback.last().toArgb(), Shader.TileMode.CLAMP)
    }
    canvas.drawCircle(c, c, inner, fill)
    return bitmap
}

/** Punto azul de "estás aquí". */
fun userDotBitmap(density: Float): Bitmap {
    val size = (22 * density).toInt()
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    canvas.drawCircle(c, c, c, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CpColors.Cyan.copy(alpha = 0.35f).toArgb() })
    canvas.drawCircle(c, c, 7 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
    canvas.drawCircle(c, c, 5 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CpColors.Cyan.toArgb() })
    return bitmap
}
