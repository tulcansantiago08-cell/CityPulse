package com.citypulse.domain.usecase

import com.citypulse.domain.model.NearbyEvent
import java.time.LocalDateTime
import kotlin.random.Random

/** Radio máximo para una sorpresa: algo a lo que se pueda llegar caminando. */
const val SURPRISE_RADIUS_MINUTES = 30

/** Una sorpresa debe estar en curso o empezar en las próximas 3 horas. */
const val SURPRISE_WINDOW_HOURS = 3L

/** Eventos que pueden salir como sorpresa en este momento. */
fun List<NearbyEvent>.surpriseCandidates(now: LocalDateTime): List<NearbyEvent> {
    val limit = now.plusHours(SURPRISE_WINDOW_HOURS)
    return filter {
        !it.event.hasEnded(now) &&
            !it.event.start.isAfter(limit) &&
            it.walkMinutes <= SURPRISE_RADIUS_MINUTES
    }
}

/**
 * Segunda opción cuando ya salieron todos los candidatos cercanos en el tiempo: cualquier
 * evento a pie que todavía no haya terminado y empiece hoy.
 */
fun List<NearbyEvent>.surpriseCandidatesLaterToday(now: LocalDateTime): List<NearbyEvent> {
    val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay()
    return filter {
        !it.event.hasEnded(now) &&
            it.event.start.isBefore(endOfDay) &&
            it.walkMinutes <= SURPRISE_RADIUS_MINUTES
    }
}

/**
 * Elige un evento al azar evitando los que ya salieron hoy. Prueba primero con [candidates] y,
 * si ya se vieron todos, con [fallback]; solo si no queda nada nuevo repite (evitando el actual).
 */
fun pickSurprise(
    candidates: List<NearbyEvent>,
    seenIds: Set<String>,
    currentId: String?,
    random: Random = Random.Default,
    fallback: List<NearbyEvent> = emptyList(),
): NearbyEvent? {
    val all = (candidates + fallback).distinctBy { it.event.id }
    if (all.isEmpty()) return null
    val pool = candidates.filter { it.event.id !in seenIds }
        .ifEmpty { fallback.filter { it.event.id !in seenIds } }
        .ifEmpty { all.filter { it.event.id != currentId } }
        .ifEmpty { all }
    return pool.random(random)
}
