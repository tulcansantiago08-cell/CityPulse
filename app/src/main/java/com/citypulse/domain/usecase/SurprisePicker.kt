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
 * Elige un evento al azar evitando los que ya salieron hoy. Si ya se vieron todos,
 * vuelve a elegir entre todos menos el actual.
 */
fun pickSurprise(
    candidates: List<NearbyEvent>,
    seenIds: Set<String>,
    currentId: String?,
    random: Random = Random.Default,
): NearbyEvent? {
    if (candidates.isEmpty()) return null
    val unseen = candidates.filter { it.event.id !in seenIds }
    val pool = unseen.ifEmpty { candidates.filter { it.event.id != currentId }.ifEmpty { candidates } }
    return pool.random(random)
}
