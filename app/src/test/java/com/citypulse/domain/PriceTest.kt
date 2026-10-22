package com.citypulse.domain

import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.model.fitsBudget
import com.citypulse.domain.model.parsePriceLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PriceTest {

    private val now = LocalDateTime.of(2026, 10, 13, 18, 0)
    private val eur = BudgetScale.forCurrency("EUR")

    @Test
    fun `lee precio y moneda de los textos del catalogo`() {
        assertEquals(15.0 to "EUR", parsePriceLabel("15 €"))
        assertEquals(40_000.0 to "COP", parsePriceLabel("Desde 40.000 COP"))
        assertEquals(8.5 to "EUR", parsePriceLabel("8,50 €"))
        assertNull(parsePriceLabel("Consultar precio"))
        assertNull(parsePriceLabel(null))
    }

    @Test
    fun `presupuesto en euros con los gratuitos siempre dentro`() {
        val free = testEvent("free", start = now)
        val cheap = testEvent("6", start = now, price = "6 €", priceMin = 6.0)
        val mid = testEvent("15", start = now, price = "15 €", priceMin = 15.0)
        val unknown = testEvent("?", start = now, price = "x", priceMin = null)

        assertTrue(free.fitsBudget(0.0, "EUR"))
        assertFalse(cheap.fitsBudget(0.0, "EUR"))
        assertTrue(cheap.fitsBudget(10.0, "EUR"))
        assertTrue(mid.fitsBudget(15.0, "EUR"))
        assertFalse(mid.fitsBudget(10.0, "EUR"))
        // Sin límite entra todo; sin precio conocido no se puede asegurar que quepa.
        assertTrue(unknown.fitsBudget(null, "EUR"))
        assertFalse(unknown.fitsBudget(100.0, "EUR"))
    }

    @Test
    fun `cada pais usa su moneda y la misma cantidad de pasos`() {
        val bogota = BudgetScale.forCurrency(BudgetScale.currencyFor("Colombia"))
        assertEquals("COP", bogota.currency)
        assertEquals(eur.unlimitedStep, bogota.unlimitedStep)
        assertEquals(50_000.0, bogota.maxFor(5)!!, 0.0)
        assertNull(bogota.maxFor(bogota.unlimitedStep))
        val show = testEvent("teatro", start = now, price = "40.000 COP", priceMin = 40_000.0, currency = "COP")
        assertTrue(show.fitsBudget(50_000.0, "COP"))
        // Un precio en otra moneda no se compara.
        assertFalse(show.fitsBudget(50.0, "EUR"))
    }
}
