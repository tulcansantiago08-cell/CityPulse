package com.citypulse.domain.repository

import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface LocationRepository {
    /** Última ubicación conocida del dispositivo, o `null` si no hay permiso o no está disponible. */
    suspend fun currentLocation(): GeoPoint?

    /**
     * Ubicación en tiempo real: emite la actual y después cada vez que el usuario se mueve.
     * Por defecto solo la actual; emite `null` si no hay permiso.
     */
    fun observeLocation(): Flow<GeoPoint?> = flow { emit(currentLocation()) }

    /**
     * Como [observeLocation] pero con GPS y cada pocos segundos, para seguir una ruta a pie.
     * Gasta más batería: solo mientras se muestra la ruta.
     */
    fun observePreciseLocation(): Flow<GeoPoint?> = observeLocation()

    /** Ciudad correspondiente a una coordenada, si se puede resolver. */
    suspend fun cityAt(point: GeoPoint): City?

    /** Lugares de cualquier parte del mundo que coinciden con un nombre ("Cusco", "Tokio"). */
    suspend fun searchPlaces(query: String): List<City>
}
