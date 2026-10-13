package com.citypulse.domain.model

/** Ciudad seleccionable. [center] se usa como referencia cuando no hay ubicación del usuario. */
data class City(
    val name: String,
    val country: String,
    val center: GeoPoint,
)
