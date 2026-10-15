package com.citypulse.domain.model

import java.time.ZoneId

/**
 * Ciudad seleccionable. [center] se usa como referencia cuando no hay ubicación del usuario.
 * Los horarios de sus eventos están en su zona horaria, [zone].
 */
data class City(
    val name: String,
    val country: String,
    val center: GeoPoint,
    val zone: ZoneId = ZoneId.systemDefault(),
)
