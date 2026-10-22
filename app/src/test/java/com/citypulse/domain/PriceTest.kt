package com.citypulse.domain

import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.fitsBudget
import com.citypulse.domain.model.parsePriceLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        assertEquals(3_000.0 to "JPY", parsePriceLabel("3.000 JPY"))
        assertEquals(25.0 to "PEN", parsePriceLabel("Desde 25 PEN"))
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
        val bogota = BudgetScale.forCity(City("Bogotá", "Colombia", GeoPoint(4.71, -74.07)))
        assertEquals("COP", bogota.currency)
        assertEquals(eur.unlimitedStep, bogota.unlimitedStep)
        // Paso base ≈ 5 € redondeado: 20.000 COP.
        assertEquals(100_000.0, bogota.maxFor(5)!!, 0.0)
        assertNull(bogota.maxFor(bogota.unlimitedStep))
        val show = testEvent("teatro", start = now, price = "40.000 COP", priceMin = 40_000.0, currency = "COP")
        assertTrue(show.fitsBudget(50_000.0, "COP"))
        // Un precio en otra moneda no se compara.
        assertFalse(show.fitsBudget(50.0, "EUR"))
    }

    @Test
    fun `funciona en cualquier pais aunque no este en la lista de ciudades`() {
        val tokyo = City("Tokio", "Japón", GeoPoint(35.68, 139.69))
        assertEquals("JPY", tokyo.currency)
        assertEquals(listOf(0.0, 1_000.0, 2_000.0), BudgetScale.forCity(tokyo).amounts.take(3))
        assertEquals("PEN", City("Cusco", "Perú", GeoPoint(-13.53, -71.97)).currency)
        assertEquals("GBP", City("London", "United Kingdom", GeoPoint(51.5, -0.12)).currency)
        // Con el código ISO del GPS no hace falta el nombre del país.
        assertEquals("ZAR", City("Ciudad del Cabo", "", GeoPoint(-33.9, 18.4), countryCode = "ZA").currency)
        // Un país desconocido no rompe nada: se usa el dólar.
        assertEquals("USD", City("Lugar", "Atlántida", GeoPoint(0.0, 0.0)).currency)
    }

    @Test
    fun `el mismo nombre en distintos paises no comparte cache`() {
        val spain = City("Valencia", "España", GeoPoint(39.47, -0.38))
        val venezuela = City("Valencia", "Venezuela", GeoPoint(10.16, -68.0))
        assertNotEquals(spain.key, venezuela.key)
    }
}
