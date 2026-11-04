package com.citypulse.domain

import com.citypulse.domain.usecase.directionsPlaceQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectionsPlaceQueryTest {
    @Test
    fun `une el lugar y la ciudad sin las aclaraciones`() {
        assertEquals("Centro de Eventos Andino, Pasto", directionsPlaceQuery("Centro de Eventos Andino", "Pasto"))
        assertEquals("Banco de la República, Pasto", directionsPlaceQuery("Banco de la República (3.er piso)", " Pasto "))
    }

    @Test
    fun `sin lugar o sin ciudad se usan las coordenadas`() {
        assertNull(directionsPlaceQuery("Café Central", null))
        assertNull(directionsPlaceQuery("Café Central", " "))
        assertNull(directionsPlaceQuery("  ", "Pasto"))
    }
}
