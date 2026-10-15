package com.citypulse.data

import com.citypulse.data.repository.mergeEvents
import com.citypulse.domain.testEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class EventMergingTest {

    private val now = LocalDateTime.of(2026, 10, 13, 18, 0)

    @Test
    fun `quita duplicados por titulo y dia dando prioridad a la primera fuente`() {
        val catalog = listOf(testEvent("cp", start = now.plusHours(1)).copy(title = "Feria del Libro"))
        val ticketmaster = listOf(
            testEvent("tm", start = now.plusHours(2)).copy(title = "FERIA DEL LIBRO"),
            testEvent("tm2", start = now.plusDays(1)).copy(title = "Feria del libro"),
        )
        assertEquals(listOf("cp", "tm2"), mergeEvents(listOf(catalog, ticketmaster), now).map { it.id })
    }

    @Test
    fun `descarta terminados y los que estan fuera del horizonte y ordena por inicio`() {
        val events = listOf(
            testEvent("later", start = now.plusDays(2)),
            testEvent("ended", start = now.minusHours(4), end = now.minusHours(1)),
            testEvent("far", start = now.plusDays(9)),
            testEvent("live", start = now.minusHours(1)),
        )
        assertEquals(listOf("live", "later"), mergeEvents(listOf(events), now).map { it.id })
    }
}
