package com.citypulse.domain.usecase

import com.citypulse.domain.text.normalizedForSearch
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.time.windowFor
import java.time.LocalDateTime

/**
 * Aplica los filtros que dedujo la IA sobre los eventos reales de la ciudad (próximos 7 días).
 * La IA solo interpreta la petición; los resultados siempre salen de los datos, nunca los inventa.
 */
fun List<NearbyEvent>.matching(intent: SearchIntent, now: LocalDateTime, currency: String): List<NearbyEvent> {
    val limit = now.toLocalDate().plusDays(7).atStartOfDay()
    val keywords = intent.keywords.map { it.normalizedForSearch().trim() }.filter { it.length >= 3 }
    return filter { nearby ->
        val e = nearby.event
        !e.hasEnded(now) && e.start.isBefore(limit) &&
            (intent.categories.isEmpty() || e.category in intent.categories) &&
            (!intent.freeOnly || e.isFree) &&
            (intent.maxPrice == null || e.isFree || (e.priceMin != null && e.currency == currency && e.priceMin <= intent.maxPrice)) &&
            (!intent.todayOnly || !e.start.toLocalDate().isAfter(now.toLocalDate())) &&
            (!intent.liveNow || e.isLive(now)) &&
            (intent.maxWalkMinutes == null || nearby.walkMinutes <= intent.maxWalkMinutes) &&
            (intent.timeSlot == null || run {
                // La franja se mira el día del evento (o hoy, si ya está en marcha).
                val day = if (e.start.isBefore(now)) now else e.start.toLocalDate().atTime(12, 0)
                intent.timeSlot.windowFor(day).overlaps(e.start, e.end)
            }) &&
            (keywords.isEmpty() || run {
                val text = "${e.title} ${e.venue} ${e.area} ${e.description}".normalizedForSearch()
                keywords.any { it in text }
            })
    }.nextOccurrences()
        .sortedWith(compareByDescending<NearbyEvent> { it.event.isLive(now) }.thenBy { it.event.start }.thenBy { it.walkMinutes })
}

/** Búsqueda de respaldo sin IA: cada palabra de la consulta debe aparecer en el evento. */
fun List<NearbyEvent>.matchingText(query: String, now: LocalDateTime): List<NearbyEvent> {
    val words = query.normalizedForSearch().split(Regex("\\s+")).filter { it.length >= 3 }
    if (words.isEmpty()) return emptyList()
    val limit = now.toLocalDate().plusDays(7).atStartOfDay()
    return filter { nearby ->
        val e = nearby.event
        val text = "${e.title} ${e.venue} ${e.area} ${e.category.label} ${e.description}".normalizedForSearch()
        !e.hasEnded(now) && e.start.isBefore(limit) && words.all { it in text }
    }.nextOccurrences()
        .sortedWith(compareByDescending<NearbyEvent> { it.event.isLive(now) }.thenBy { it.event.start })
}
