package com.citypulse.domain

import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.usecase.matching
import com.citypulse.domain.usecase.matchingText
import com.citypulse.domain.usecase.withDistanceFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SearchIntentMatchingTest {

    private val today = LocalDate.of(2026, 10, 13)
    private val now = today.atTime(18, 0)
    private val events = SampleData.events(today).withDistanceFrom(SampleData.PuertaDelSol)

    private fun ids(intent: SearchIntent) = events.matching(intent, now, "EUR").map { it.event.id }

    @Test
    fun `musica gratis esta noche`() {
        val result = ids(SearchIntent(categories = setOf(Category.Music), freeOnly = true, timeSlot = TimeSlot.Night, todayOnly = true))
        assertEquals(listOf("dj"), result)
    }

    @Test
    fun `tope de precio incluye gratis y excluye lo caro`() {
        val result = ids(SearchIntent(categories = setOf(Category.Music), todayOnly = true, maxPrice = 20.0))
        assertTrue("jazz" in result) // 15 €
        assertTrue("dj" in result) // gratis
        assertTrue("flamenco" !in result) // 25 €
    }

    @Test
    fun `palabras clave buscan en titulo y descripcion sin tildes`() {
        assertEquals(listOf("flamenco"), ids(SearchIntent(keywords = listOf("FLAMENCO"))))
    }

    @Test
    fun `en curso y a pocos minutos`() {
        val result = events.matching(SearchIntent(liveNow = true, maxWalkMinutes = 12), now, "EUR")
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.event.isLive(now) && it.walkMinutes <= 12 })
    }

    @Test
    fun `busqueda de respaldo sin ia por texto`() {
        assertEquals(listOf("rastro"), events.matchingText("rastro", now).map { it.event.id })
        assertTrue(events.matchingText("xy", now).isEmpty())
    }
}
