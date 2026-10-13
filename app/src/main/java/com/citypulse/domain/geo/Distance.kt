package com.citypulse.domain.geo

import com.citypulse.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Velocidad media caminando (≈ 4,8 km/h). */
const val WALKING_METERS_PER_MINUTE = 80.0

/** Las calles no van en línea recta: se corrige la distancia en línea recta con este factor. */
const val STREET_DETOUR_FACTOR = 1.3

/** Distancia en línea recta entre dos puntos (fórmula del haversine). */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

/** Minutos estimados a pie para una distancia en línea recta. Nunca menos de 1. */
fun walkingMinutes(straightLineMeters: Double): Int =
    max(1, (straightLineMeters * STREET_DETOUR_FACTOR / WALKING_METERS_PER_MINUTE).roundToInt())
