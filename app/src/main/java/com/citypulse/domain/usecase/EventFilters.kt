package com.citypulse.domain.usecase

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.geo.walkingMinutes
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.time.windowFor
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Un evento que empieza dentro de este margen ya se muestra en "Pasando ahora". */
const val STARTING_SOON_MINUTES = 60L

fun List<Event>.withDistanceFrom(origin: GeoPoint): List<NearbyEvent> = map { event ->
    val meters = distanceMeters(origin, event.location)
    NearbyEvent(event, meters.roundToInt(), walkingMinutes(meters))
}

/** Filtros de la pantalla de inicio: "Solo gratis" y categoría (`null` = todas). */
fun Event.matches(freeOnly: Boolean, category: Category?): Boolean =
    (!freeOnly || isFree) && (category == null || this.category == category)

/** Eventos en curso o que empiezan en menos de una hora, por hora de inicio. */
fun List<NearbyEvent>.happeningNow(now: LocalDateTime): List<NearbyEvent> {
    val soon = now.plusMinutes(STARTING_SOON_MINUTES)
    return filter { !it.event.hasEnded(now) && !it.event.start.isAfter(soon) }
        .sortedBy { it.event.start }
}

/** Próximos eventos de los siguientes 7 días que no están ya en "Pasando ahora". */
fun List<NearbyEvent>.upcomingThisWeek(now: LocalDateTime): List<NearbyEvent> {
    val soon = now.plusMinutes(STARTING_SOON_MINUTES)
    val limit = now.toLocalDate().plusDays(7).atStartOfDay()
    return filter { it.event.start.isAfter(soon) && it.event.start.isBefore(limit) }
        .sortedBy { it.event.start }
}

/**
 * Modo "Estoy aquí ahora": eventos sin terminar que coinciden con la franja elegida y quedan
 * a [radiusMinutes] o menos a pie, ordenados por cercanía.
 */
fun List<NearbyEvent>.nearbyInSlot(now: LocalDateTime, slot: TimeSlot, radiusMinutes: Int): List<NearbyEvent> {
    val window = slot.windowFor(now)
    return filter {
        !it.event.hasEnded(now) &&
            it.walkMinutes <= radiusMinutes &&
            window.overlaps(it.event.start, it.event.end)
    }.sortedWith(compareBy({ it.walkMinutes }, { it.event.start }))
}
