package com.citypulse.domain.usecase

import com.citypulse.domain.geo.STREET_DETOUR_FACTOR
import com.citypulse.domain.geo.WALKING_METERS_PER_MINUTE
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.geo.walkingMinutes
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import kotlin.math.ceil
import kotlin.math.max

/** Si el usuario se aleja más que esto del punto donde se calculó la ruta, se recalcula. */
const val REROUTE_METERS = 40.0

/** Los modos no elegidos solo actualizan su tiempo tras un cambio grande de posición. */
const val REFRESH_OTHER_MODES_METERS = 1_000.0

/** A menos de esta distancia del destino se considera que ya llegó. */
const val ARRIVED_METERS = 30.0

/** Más lejos que esto en línea recta, la ruta se abre en carro en vez de a pie. */
const val WALKABLE_METERS = 2_000.0

/** Minutos a pie para una distancia ya medida por las calles (mismo ritmo que en las tarjetas). */
fun walkingMinutesAlongStreets(meters: Double): Int = max(1, ceil(meters / WALKING_METERS_PER_MINUTE).toInt())

/** Modo con el que se abre la ruta, según lo lejos que esté el usuario. */
fun defaultTravelMode(from: GeoPoint, to: GeoPoint): TravelMode =
    if (distanceMeters(from, to) <= WALKABLE_METERS) TravelMode.WALK else TravelMode.CAR

/** "25 min", "1 h 5 min" o "3 h". */
fun formatTravelTime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$minutes min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

/** Línea recta para mostrar mientras no hay ruta por las calles (sin conexión, sin camino…). */
fun straightLineRoute(from: GeoPoint, to: GeoPoint, reachable: Boolean = true): TravelRoute {
    val straight = distanceMeters(from, to)
    return TravelRoute(
        points = listOf(from, to),
        distanceMeters = straight * STREET_DETOUR_FACTOR,
        minutes = walkingMinutes(straight),
        exact = false,
        reachable = reachable,
    )
}
