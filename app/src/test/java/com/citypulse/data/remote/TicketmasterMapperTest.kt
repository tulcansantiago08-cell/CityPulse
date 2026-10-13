package com.citypulse.data.remote

import com.citypulse.data.remote.ticketmaster.TmEventsResponse
import com.citypulse.data.remote.ticketmaster.toDomain
import com.citypulse.data.remote.ticketmaster.toTicketmasterUtc
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.EventSource
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TicketmasterMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val response: TmEventsResponse = json.decodeFromString(
        javaClass.classLoader!!.getResource("ticketmaster_events.json").readText(),
    )
    private val events = response.embedded!!.events

    @Test
    fun `mapea un concierto completo`() {
        val event = events[0].toDomain()!!
        assertEquals("tm_Z698xZ", event.id)
        assertEquals(Category.Music, event.category)
        assertEquals(LocalDateTime.of(2026, 10, 13, 21, 0), event.start)
        assertEquals(LocalDateTime.of(2026, 10, 13, 23, 0), event.end) // sin fin: 2 h
        assertEquals("Sala Ejemplo", event.venue)
        assertEquals("Calle Mayor 1", event.area)
        assertEquals(40.4169, event.location.latitude, 1e-6)
        assertFalse(event.isFree)
        assertEquals("Desde 15 €", event.price)
        assertEquals("https://img/large.jpg", event.imageUrl)
        assertEquals(EventSource.Ticketmaster, event.source)
    }

    @Test
    fun `evento sin hora usa horario de dia y precio cero es gratis`() {
        val event = events[1].toDomain()!!
        assertEquals(Category.Fairs, event.category)
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), event.start)
        assertEquals(LocalDateTime.of(2026, 10, 14, 20, 0), event.end)
        assertTrue(event.isFree)
        assertNull(event.price)
    }

    @Test
    fun `descarta eventos sin recinto`() {
        assertNull(events[2].toDomain())
    }

    @Test
    fun `fechas de consulta en UTC`() {
        val madrid = ZoneId.of("Europe/Madrid")
        assertEquals("2026-10-13T19:00:00Z", LocalDateTime.of(2026, 10, 13, 21, 0).toTicketmasterUtc(madrid))
    }
}
