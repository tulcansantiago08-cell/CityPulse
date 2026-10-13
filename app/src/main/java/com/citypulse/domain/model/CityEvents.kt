package com.citypulse.domain.model

/**
 * Eventos de la ciudad seleccionada con su distancia al punto de referencia.
 *
 * @property usingUserLocation `true` si [origin] es la ubicación del usuario; `false` si es el centro de la ciudad.
 */
data class CityEvents(
    val city: City,
    val origin: GeoPoint,
    val usingUserLocation: Boolean,
    val events: List<NearbyEvent>,
)
