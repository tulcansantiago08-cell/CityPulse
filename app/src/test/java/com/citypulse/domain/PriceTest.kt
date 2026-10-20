package com.citypulse.domain

import com.citypulse.domain.model.PriceBands
import com.citypulse.domain.model.PriceRange
import com.citypulse.domain.model.isInPriceRange
import com.citypulse.domain.model.parsePriceLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PriceTest {

    private val now = LocalDateTime.of(2026, 10, 13, 18, 0)
    private val eur = PriceBands.forCurrency("EUR")

    @Test
    fun `lee precio y moneda de los textos del catalogo`() {
        assertEquals(15.0 to "EUR", parsePriceLabel("15 €"))
        assertEquals(40_000.0 to "COP", parsePriceLabel("Desde 40.000 COP"))
        assertEquals(8.5 to "EUR", parsePriceLabel("8,50 €"))
        assertNull(parsePriceLabel("Consultar precio"))
        assertNull(parsePriceLabel(null))
    }

    @Test
    fun `rangos en euros con gratis como el mas barato`() {
        val free = testEvent("free", start = now)
        val cheap = testEvent("6", start = now, price = "6 €", priceMin = 6.0)
        val mid = testEvent("15", start = now, price = "15 €", priceMin = 15.0)
        val high = testEvent("45", start = now, price = "45 €", priceMin = 45.0)
        val unknown = testEvent("?", start = now, price = "x", priceMin = null)

        assertTrue(free.isInPriceRange(PriceRange.Low, eur))
        assertTrue(cheap.isInPriceRange(PriceRange.Low, eur))
        assertTrue(mid.isInPriceRange(PriceRange.Mid, eur))
        assertTrue(high.isInPriceRange(PriceRange.High, eur))
        assertFalse(mid.isInPriceRange(PriceRange.Low, eur))
        // Sin precio conocido solo aparece con "Todos".
        assertFalse(unknown.isInPriceRange(PriceRange.Low, eur))
        assertTrue(unknown.isInPriceRange(PriceRange.Any, eur))
    }

    @Test
    fun `cada pais usa su moneda y sus limites`() {
        val bogota = PriceBands.forCurrency(PriceBands.currencyFor("Colombia"))
        assertEquals("COP", bogota.currency)
        val show = testEvent("teatro", start = now, price = "40.000 COP", priceMin = 40_000.0, currency = "COP")
        assertTrue(show.isInPriceRange(PriceRange.Mid, bogota))
        // Un precio en otra moneda no se compara.
        assertFalse(show.isInPriceRange(PriceRange.High, eur))
    }
}
