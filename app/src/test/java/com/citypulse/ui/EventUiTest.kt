package com.citypulse.ui

import com.citypulse.domain.PuertaDelSol
import com.citypulse.domain.testEvent
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.toUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class EventUiTest {

    private val now = LocalDateTime.of(2026, 10, 13, 18, 0)

    private fun label(price: String?, free: Boolean, url: String?) =
        listOf(testEvent(start = now, price = price).copy(isFree = free, url = url))
            .withDistanceFrom(PuertaDelSol).single().toUi(now)

    @Test
    fun `el precio se describe segun lo que se sabe`() {
        assertEquals("Gratis", label(price = null, free = true, url = null).priceLabel)
        assertEquals("15 €", label(price = "15 €", free = false, url = "https://x").priceLabel)
        assertEquals("En taquilla", label(price = null, free = false, url = null).priceLabel)

        val online = label(price = null, free = false, url = "https://www.ticketmaster.es/event/1")
        assertEquals("Ver precio", online.priceLabel)
        assertTrue(online.priceOnline)
        assertFalse(label(price = "15 €", free = false, url = "https://x").priceOnline)
    }

    @Test
    fun `siempre hay forma de conseguir entradas salvo en los gratuitos`() {
        assertEquals(null, label(price = null, free = true, url = null).ticketsUrl)
        assertEquals("https://tm/1", label(price = "15 €", free = false, url = "https://tm/1").ticketsUrl)
        val search = label(price = "8 €", free = false, url = null).ticketsUrl!!
        assertTrue(search.startsWith("https://www.google.com/search?q=Evento"))
        assertTrue("entradas" in search)
    }

    @Test
    fun `fecha absoluta para compartir y estado finalizado`() {
        val past = listOf(testEvent("ayer", start = now.minusDays(1), end = now.minusDays(1).plusHours(2)))
            .withDistanceFrom(PuertaDelSol).single().toUi(now)
        assertTrue(past.ended)
        assertEquals("lunes 12 oct · 18:00", past.absoluteWhen)
        assertFalse(label(price = null, free = true, url = null).ended)
    }
}
