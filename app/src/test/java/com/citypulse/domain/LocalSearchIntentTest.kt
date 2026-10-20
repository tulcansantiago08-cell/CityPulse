package com.citypulse.domain

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.usecase.localSearchIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchIntentTest {

    @Test
    fun `algo gratis para esta noche`() {
        val intent = localSearchIntent("Algo gratis para esta noche")
        assertTrue(intent.freeOnly)
        assertTrue(intent.todayOnly)
        assertEquals(TimeSlot.Night, intent.timeSlot)
        assertTrue(intent.keywords.isEmpty())
    }

    @Test
    fun `musica en vivo cerca`() {
        val intent = localSearchIntent("Música en vivo a menos de 15 minutos, cerca")
        assertEquals(setOf(Category.Music), intent.categories)
        assertEquals(15, intent.maxWalkMinutes)
    }

    @Test
    fun `mañana sola no es una franja pero por la mañana si`() {
        assertEquals(null, localSearchIntent("mercado mañana").timeSlot)
        assertEquals(TimeSlot.Morning, localSearchIntent("museo por la mañana").timeSlot)
    }

    @Test
    fun `nombre de un lugar se busca como palabra clave`() {
        assertEquals(listOf("matadero"), localSearchIntent("Matadero").keywords)
    }
}
