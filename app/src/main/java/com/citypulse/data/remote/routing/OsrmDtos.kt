package com.citypulse.data.remote.routing

import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import com.citypulse.domain.usecase.walkingMinutesAlongStreets
import kotlin.math.ceil
import kotlin.math.max
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
    /** Segundos. */
    val duration: Double = 0.0,
    val geometry: OsrmGeometry,
)

/** GeoJSON LineString: cada punto es [longitud, latitud]. */
@Serializable
data class OsrmGeometry(val coordinates: List<List<Double>>)

/** Primera ruta de la respuesta, o `null` si OSRM no encontró camino. */
fun OsrmResponse.toTravelRoute(mode: TravelMode): TravelRoute? {
    if (code != "Ok") return null
    val route = routes.firstOrNull() ?: return null
    val points = route.geometry.coordinates.mapNotNull { if (it.size >= 2) GeoPoint(latitude = it[1], longitude = it[0]) else null }
    if (points.size < 2) return null
    val minutes = when (mode) {
        // A pie, con el mismo ritmo que el resto de la app (80 m/min), no el de OSRM.
        TravelMode.WALK -> walkingMinutesAlongStreets(route.distance)
        else -> max(1, ceil(route.duration / 60).toInt())
    }
    return TravelRoute(points, route.distance, minutes)
}
