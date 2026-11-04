package com.citypulse.domain

import com.citypulse.domain.usecase.reminderInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EventRemindersTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    private val bogota = ZoneId.of("America/Bogota")

    @Test
    fun `avisa una hora antes con la hora de la ciudad del evento`() {
        // 20:00 en Madrid en noviembre (UTC+1) son las 19:00 UTC: el aviso, a las 18:00 UTC.
        val start = LocalDateTime.of(2026, 11, 10, 20, 0)
        val now = Instant.parse("2026-11-10T10:00:00Z")
        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), reminderInstant(start, madrid, now))
    }

    @Test
    fun `tiene en cuenta el horario de verano`() {
        // En julio Madrid está en UTC+2: 20:00 son las 18:00 UTC y el aviso, a las 17:00 UTC.
        val start = LocalDateTime.of(2026, 7, 10, 20, 0)
        val now = Instant.parse("2026-07-01T00:00:00Z")
        assertEquals(Instant.parse("2026-07-10T17:00:00Z"), reminderInstant(start, madrid, now))
    }

    @Test
    fun `la misma hora local en otra ciudad avisa en otro momento`() {
        val start = LocalDateTime.of(2026, 11, 10, 20, 0)
        val now = Instant.parse("2026-11-10T10:00:00Z")
        // 20:00 en Bogotá (UTC-5) son la 01:00 UTC del día siguiente.
        assertEquals(Instant.parse("2026-11-11T00:00:00Z"), reminderInstant(start, bogota, now))
    }

    @Test
    fun `no programa avisos que ya pasaron`() {
        val start = LocalDateTime.of(2026, 11, 10, 20, 0)
        // 19:30 en Madrid: faltan 30 minutos, el momento de avisar ya pasó.
        val now = start.minusMinutes(30).atZone(madrid).toInstant()
        assertNull(reminderInstant(start, madrid, now))
        // Con antelación cero sirve para saber si el evento todavía no empezó.
        assertEquals(start.atZone(madrid).toInstant(), reminderInstant(start, madrid, now, leadMinutes = 0))
    }
}
