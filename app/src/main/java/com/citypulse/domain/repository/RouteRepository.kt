package com.citypulse.domain.repository

import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute

/** Rutas por las calles. */
interface RouteRepository {
    /**
     * Falla con [com.citypulse.domain.model.NoRouteException] si no hay camino, y con otra
     * excepción si no hay conexión o el servicio de rutas no responde.
     */
    suspend fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): Result<TravelRoute>
}
