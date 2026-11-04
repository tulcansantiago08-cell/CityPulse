package com.citypulse.domain.repository

import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute

/** Rutas a pie por las calles. */
interface RouteRepository {
    /** Falla si no hay conexión o el servicio de rutas no responde. */
    suspend fun walkingRoute(from: GeoPoint, to: GeoPoint): Result<WalkingRoute>
}
