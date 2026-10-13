package com.citypulse.domain.model

import java.time.LocalDateTime

/** Origen de un evento: la API de Ticketmaster o el catálogo propio en Firestore. */
enum class EventSource { Ticketmaster, CityPulse }

/**
 * Evento de la ciudad. Las fechas están en la hora local de la ciudad.
 *
 * @property price precio legible ("15 €"); `null` cuando el evento es gratuito.
 */
data class Event(
    val id: String,
    val title: String,
    val category: Category,
    val venue: String,
    val area: String,
    val location: GeoPoint,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val price: String?,
    val description: String,
    val imageUrl: String?,
    val source: EventSource,
    val url: String? = null,
) {
    val isFree: Boolean get() = price == null

    /** El evento ya empezó y todavía no termina. */
    fun isLive(now: LocalDateTime): Boolean = !now.isBefore(start) && now.isBefore(end)

    fun hasEnded(now: LocalDateTime): Boolean = !now.isBefore(end)
}
