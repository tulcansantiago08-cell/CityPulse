package com.citypulse.domain.repository

import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint

interface LocationRepository {
    /** Última ubicación conocida del dispositivo, o `null` si no hay permiso o no está disponible. */
    suspend fun currentLocation(): GeoPoint?

    /** Ciudad correspondiente a una coordenada, si se puede resolver. */
    suspend fun cityAt(point: GeoPoint): City?
}
