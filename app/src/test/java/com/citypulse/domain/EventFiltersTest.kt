package com.citypulse.domain

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.usecase.happeningNow
import com.citypulse.domain.usecase.matches
import com.citypulse.domain.usecase.nearbyInSlot
import com.citypulse.domain.usecase.upcomingThisWeek
import com.citypulse.domain.usecase.withDistanceFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class EventFiltersTest {

    private val now = LocalDateTime.of(2026, 10, 13, 21, 0)

    @Test
    fun `pasando ahora incluye en curso y los que empiezan en menos de una hora`() {
        val events = listOf(
            testEvent("live", start = now.minusHours(1)),
            testEvent("soon", start = now.plusMinutes(45)),
            testEvent("later", start = now.plusHours(3)),
            testEvent("ended", start = now.minusHours(3), end = now.minusHours(1)),
        ).withDistanceFrom(PuertaDelSol)

        assertEquals(listOf("live", "soon"), events.happeningNow(now).map { it.event.id })
        assertEquals(listOf("later"), events.upcomingThisWeek(now).map { it.event.id })
    }

    @Test
    fun `esta semana excluye eventos a mas de 7 dias`() {
        val events = listOf(
            testEvent("d6", start = now.plusDays(6)),
            testEvent("d8", start = now.plusDays(8)),
        ).withDistanceFrom(PuertaDelSol)
        assertEquals(listOf("d6"), events.upcomingThisWeek(now).map { it.event.id })
    }

    @Test
    fun `cerca de ti filtra por franja y radio y ordena por cercania`() {
        val near = GeoPoint(40.4172, -3.7030)   // ~1 min
        val mid = GeoPoint(40.4142, -3.7006)    // ~6 min
        val far = GeoPoint(40.3920, -3.6975)    // ~45 min
        val events = listOf(
            testEvent("mid", start = now, location = mid),
            testEvent("near", start = now.plusHours(1), location = near),
            testEvent("far", start = now, location = far),
            testEvent("morning", start = now.plusDays(1).withHour(9), location = near),
        ).withDistanceFrom(PuertaDelSol)

        val result = events.nearbyInSlot(now, TimeSlot.Night, radiusMinutes = 15)
        assertEquals(listOf("near", "mid"), result.map { it.event.id })
    }

    @Test
    fun `filtro gratis y categoria`() {
        val free = testEvent("a", start = now, category = Category.Markets)
        val paid = testEvent("b", start = now, price = "10 €")
        assertTrue(free.matches(freeOnly = true, category = null))
        assertFalse(paid.matches(freeOnly = true, category = null))
        assertTrue(free.matches(freeOnly = false, category = Category.Markets))
        assertFalse(free.matches(freeOnly = false, category = Category.Music))
    }
}
