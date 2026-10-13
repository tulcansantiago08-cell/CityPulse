package com.citypulse.domain

import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.time.greetingFor
import com.citypulse.domain.time.timeSlotFor
import com.citypulse.domain.time.windowFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class DayPeriodsTest {

    @Test
    fun `franjas siguen los cortes del prototipo`() {
        assertEquals(TimeSlot.Night, timeSlotFor(LocalTime.of(5, 59)))
        assertEquals(TimeSlot.Morning, timeSlotFor(LocalTime.of(6, 0)))
        assertEquals(TimeSlot.Morning, timeSlotFor(LocalTime.of(13, 59)))
        assertEquals(TimeSlot.Afternoon, timeSlotFor(LocalTime.of(14, 0)))
        assertEquals(TimeSlot.Afternoon, timeSlotFor(LocalTime.of(19, 59)))
        assertEquals(TimeSlot.Night, timeSlotFor(LocalTime.of(20, 0)))
    }

    @Test
    fun `saludo cambia a las 6, 13 y 20`() {
        assertEquals(Greeting.Evening, greetingFor(LocalTime.of(5, 0)))
        assertEquals(Greeting.Morning, greetingFor(LocalTime.of(6, 0)))
        assertEquals(Greeting.Afternoon, greetingFor(LocalTime.of(13, 0)))
        assertEquals(Greeting.Evening, greetingFor(LocalTime.of(20, 0)))
    }

    @Test
    fun `de madrugada la noche pertenece al dia anterior`() {
        val now = LocalDateTime.of(2026, 10, 14, 2, 0)
        val window = TimeSlot.Night.windowFor(now)
        assertEquals(LocalDateTime.of(2026, 10, 13, 20, 0), window.start)
        assertEquals(LocalDateTime.of(2026, 10, 14, 6, 0), window.end)
    }
}
