package com.citypulse.domain.model

/**
 * Camino a pie entre dos puntos, siguiendo las calles.
 *
 * [exact] es `false` cuando no se pudo calcular por las calles y es solo la línea recta
 * (con la distancia corregida por el trazado de las calles, como en las tarjetas).
 */
data class WalkingRoute(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val minutes: Int,
    val exact: Boolean = true,
)
