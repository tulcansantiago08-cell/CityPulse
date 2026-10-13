package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Colores de respaldo mientras carga la imagen (o si falla), igual que en el prototipo. */
val DefaultImageFallback = listOf(Color(0xFF2A1A4A), Color(0xFF0B0A10))

/**
 * Imagen de evento a sangre. Debajo se dibuja un degradado de respaldo para que la tarjeta
 * nunca quede vacía sin conexión.
 */
@Composable
fun EventImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fallback: List<Color> = DefaultImageFallback,
) {
    Box(modifier.background(Brush.linearGradient(fallback))) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
