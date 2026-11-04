package com.citypulse.domain.usecase

import com.citypulse.domain.geo.STREET_DETOUR_FACTOR
import com.citypulse.domain.geo.WALKING_METERS_PER_MINUTE
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.geo.walkingMinutes
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute
import kotlin.math.ceil
import kotlin.math.max

/** Si el usuario se aleja más que esto del punto donde se calculó la ruta, se recalcula. */
const val REROUTE_METERS = 40.0

/** A menos de esta distancia del destino se considera que ya llegó. */
const val ARRIVED_METERS = 30.0

/** Minutos a pie para una distancia ya medida por las calles (mismo ritmo que en las tarjetas). */
fun walkingMinutesAlongStreets(meters: Double): Int = max(1, ceil(meters / WALKING_METERS_PER_MINUTE).toInt())

/** Línea recta para mostrar mientras no hay ruta por las calles (sin conexión, servicio caído…). */
fun straightLineRoute(from: GeoPoint, to: GeoPoint): WalkingRoute {
    val straight = distanceMeters(from, to)
    return WalkingRoute(
        points = listOf(from, to),
        distanceMeters = straight * STREET_DETOUR_FACTOR,
        minutes = walkingMinutes(straight),
        exact = false,
    )
}
