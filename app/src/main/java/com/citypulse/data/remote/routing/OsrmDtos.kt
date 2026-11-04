package com.citypulse.data.remote.routing

import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute
import com.citypulse.domain.usecase.walkingMinutesAlongStreets
import kotlinx.serialization.Serializable

@Serializable
data class OsrmResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
data class OsrmRoute(
    /** Metros. */
    val distance: Double,
    val geometry: OsrmGeometry,
)

/** GeoJSON LineString: cada punto es [longitud, latitud]. */
@Serializable
data class OsrmGeometry(val coordinates: List<List<Double>>)

/** Primera ruta de la respuesta, o `null` si OSRM no encontró camino. */
fun OsrmResponse.toWalkingRoute(): WalkingRoute? {
    if (code != "Ok") return null
    val route = routes.firstOrNull() ?: return null
    val points = route.geometry.coordinates.mapNotNull { if (it.size >= 2) GeoPoint(latitude = it[1], longitude = it[0]) else null }
    if (points.size < 2) return null
    // Los minutos con el mismo ritmo que el resto de la app (80 m/min), no los de OSRM.
    return WalkingRoute(points, route.distance, walkingMinutesAlongStreets(route.distance))
}
