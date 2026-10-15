package com.citypulse.data.remote.ticketmaster

import com.citypulse.BuildConfig
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.GeoPoint
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Eventos de Ticketmaster alrededor de un punto en un rango de fechas. */
@Singleton
class TicketmasterEventSource @Inject constructor(
    private val api: TicketmasterApi,
) {
    val isConfigured: Boolean get() = BuildConfig.TM_API_KEY.isNotBlank()

    /** [from] y [to] están en la hora local de la ciudad ([zone]). */
    suspend fun events(center: GeoPoint, from: LocalDateTime, to: LocalDateTime, zone: ZoneId, radiusKm: Int = 15): List<Event> {
        if (!isConfigured) return emptyList()
        val response = api.searchEvents(
            latLong = String.format(Locale.US, "%.5f,%.5f", center.latitude, center.longitude),
            radiusKm = radiusKm,
            startDateTime = from.toTicketmasterUtc(zone),
            endDateTime = to.toTicketmasterUtc(zone),
        )
        return response.embedded?.events.orEmpty().mapNotNull { it.toDomain() }
    }
}
