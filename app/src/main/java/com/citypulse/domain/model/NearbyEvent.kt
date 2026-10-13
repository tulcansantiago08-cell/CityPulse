package com.citypulse.domain.model

/** Evento junto con su distancia al punto de referencia del usuario. */
data class NearbyEvent(
    val event: Event,
    val distanceMeters: Int,
    val walkMinutes: Int,
)
