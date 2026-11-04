package com.citypulse.domain.model

/** Forma de llegar a un evento. */
enum class TravelMode {
    WALK,
    BIKE,
    CAR,

    /** Sin datos de líneas ni horarios en la app: se consulta en Google Maps. */
    TRANSIT;

    val routedInApp: Boolean get() = this != TRANSIT
}

/**
 * Camino entre dos puntos.
 *
 * [exact] es `false` cuando no se pudo calcular por las calles y es solo la línea recta, con la
 * distancia corregida por el trazado de las calles. [reachable] es `false` cuando el servicio
 * respondió que no hay camino (p. ej. el usuario está en otro continente): no sirve reintentar.
 */
data class TravelRoute(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val minutes: Int,
    val exact: Boolean = true,
    val reachable: Boolean = true,
)

/** El servicio de rutas respondió, pero no hay camino entre los dos puntos. */
class NoRouteException : Exception("Sin ruta entre los dos puntos")
