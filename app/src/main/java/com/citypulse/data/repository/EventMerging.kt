package com.citypulse.data.repository

import com.citypulse.domain.text.normalizedForSearch
import com.citypulse.domain.model.Event
import java.time.LocalDateTime

/**
 * Une los eventos de varias fuentes: descarta los terminados y los de más allá del horizonte,
 * y quita duplicados (mismo título normalizado el mismo día). Ante un duplicado se queda el
 * primero, así que el orden de [sources] marca la prioridad.
 */
fun mergeEvents(sources: List<List<Event>>, now: LocalDateTime, horizonDays: Long = 7): List<Event> {
    val limit = now.toLocalDate().plusDays(horizonDays).atStartOfDay()
    val seen = HashSet<Pair<String, java.time.LocalDate>>()
    return sources.flatten()
        .filter { !it.hasEnded(now) && it.start.isBefore(limit) }
        .filter { seen.add(it.title.normalizedForSearch().trim() to it.start.toLocalDate()) }
        .sortedBy { it.start }
}
