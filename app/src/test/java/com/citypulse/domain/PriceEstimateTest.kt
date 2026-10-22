package com.citypulse.domain

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.PriceEstimate
import com.citypulse.domain.model.fitsBudget
import com.citypulse.domain.model.withLocalPriceEstimate
import com.citypulse.ui.common.toUi
import com.citypulse.domain.usecase.withDistanceFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PriceEstimateTest {

    private val now = LocalDateTime.of(2026, 10, 22, 18, 0)

    /** Evento de pago sin precio publicado, como los de Ticketmaster. */
    private fun withoutPrice(venue: String, category: Category = Category.Music) =
        testEvent("tm", start = now, category = category, price = "x", priceMin = null, currency = null)
            .copy(price = null, venue = venue, url = "https://www.ticketmaster.es/e")

    @Test
    fun `estima segun el recinto y lo marca como estimado`() {
        val arena = withoutPrice("Palau Sant Jordi").withLocalPriceEstimate("EUR")
        val club = withoutPrice("Sala Apolo").withLocalPriceEstimate("EUR")
        val museum = withoutPrice("Museu Picasso", Category.Art).withLocalPriceEstimate("EUR")
        assertEquals(55.0, arena.priceMin!!, 0.0)
        assertEquals(18.0, club.priceMin!!, 0.0)
        assertEquals(12.0, museum.priceMin!!, 0.0)
        assertEquals(PriceEstimate.Local, club.priceEstimate)
        assertEquals("EUR", club.currency)
        // Ya entra en el presupuesto como cualquier otro precio.
        assertTrue(club.fitsBudget(20.0, "EUR"))
    }

    @Test
    fun `usa la moneda de la ciudad con cifras redondas`() {
        val bogota = withoutPrice("Teatro Colón").withLocalPriceEstimate("COP")
        assertEquals("COP", bogota.currency)
        assertEquals(64_000.0, bogota.priceMin!!, 0.0)
    }

    @Test
    fun `no toca los gratuitos ni los que ya tienen precio`() {
        val free = testEvent("gratis", start = now)
        val priced = testEvent("15", start = now, price = "15 €", priceMin = 15.0)
        assertEquals(free, free.withLocalPriceEstimate("EUR"))
        assertEquals(priced, priced.withLocalPriceEstimate("EUR"))
        assertNull(priced.withLocalPriceEstimate("EUR").priceEstimate)
    }

    @Test
    fun `la tarjeta muestra el precio estimado con el simbolo aproximado`() {
        val event = withoutPrice("Sala Apolo").withLocalPriceEstimate("EUR")
        val ui = listOf(event).withDistanceFrom(PuertaDelSol).first().toUi(now)
        assertEquals("≈ 18 €", ui.priceLabel.replace(' ', ' '))
        assertEquals(PriceEstimate.Local, ui.priceEstimate)
    }
}
