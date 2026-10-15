package com.citypulse.data.remote

import com.citypulse.data.remote.firestore.CatalogEntry
import com.citypulse.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CatalogEntryTest {

    private val sundayMarket = mapOf(
        "city" to "Madrid", "title" to "El Rastro", "category" to "Markets",
        "venue" to "Ribera de Curtidores", "area" to "La Latina", "lat" to 40.4085, "lng" to -3.707,
        "startTime" to "09:00", "endTime" to "15:00", "weekdays" to listOf(7L),
        "price" to null, "free" to true, "description" to "Mercadillo",
    )

    @Test
    fun `evento recurrente genera una ocurrencia por cada dia que coincide`() {
        val entry = CatalogEntry.fromDocument("rastro", sundayMarket)!!
        val monday = LocalDate.of(2026, 10, 12)
        val events = entry.occurrences(monday, days = 14)
        assertEquals(2, events.size)
        assertEquals(LocalDateTime.of(2026, 10, 18, 9, 0), events[0].start)
        assertEquals("cp_rastro_2026-10-18", events[0].id)
        assertTrue(events.all { it.isFree && it.category == Category.Markets })
    }

    @Test
    fun `fin anterior al inicio termina al dia siguiente`() {
        val entry = CatalogEntry.fromDocument("dj", sundayMarket + mapOf("startTime" to "22:00", "endTime" to "02:00", "weekdays" to listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L)))!!
        val event = entry.occurrences(LocalDate.of(2026, 10, 13), days = 1).single()
        assertEquals(LocalDateTime.of(2026, 10, 14, 2, 0), event.end)
    }

    @Test
    fun `evento de fecha concreta solo aparece ese dia`() {
        val entry = CatalogEntry.fromDocument("feria", sundayMarket + mapOf("date" to "2026-10-15", "weekdays" to null))!!
        assertEquals(listOf(LocalDate.of(2026, 10, 15)), entry.occurrences(LocalDate.of(2026, 10, 13)).map { it.start.toLocalDate() })
    }

    @Test
    fun `documento incompleto se descarta`() {
        assertNull(CatalogEntry.fromDocument("x", sundayMarket - "lat"))
    }
}
