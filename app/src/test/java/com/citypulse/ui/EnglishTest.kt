package com.citypulse.ui

import com.citypulse.domain.PuertaDelSol
import com.citypulse.domain.i18n.AppLanguage
import com.citypulse.domain.i18n.AppLocale
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.testEvent
import com.citypulse.domain.usecase.localSearchIntent
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.CategoryOptions
import com.citypulse.ui.common.dateLine
import com.citypulse.ui.common.dayLabel
import com.citypulse.ui.common.distanceLabel
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** En inglés la app escribe sus textos a la inglesa y la búsqueda sin IA entiende inglés. */
class EnglishTest {
    private val now = LocalDateTime.of(2026, 10, 13, 18, 0) // martes

    @After fun spanishAgain() {
        AppLocale.language = AppLanguage.ES
    }

    @Test fun `en espanol todo sigue igual`() {
        assertEquals("Hoy", now.toLocalDate().dayLabel(now.toLocalDate()))
        assertEquals("Martes 13 oct · 18:00", now.dateLine())
        assertEquals("0,5 km · 6 min a pie", distanceLabel(500, 6))
        assertEquals("Música", Category.Music.displayLabel)
        assertEquals("Todo", CategoryOptions.first().label)
    }

    @Test fun `en ingles cambian fechas, distancias, etiquetas y precios`() {
        AppLocale.language = AppLanguage.EN
        assertEquals("Today", now.toLocalDate().dayLabel(now.toLocalDate()))
        assertEquals("Tomorrow", now.toLocalDate().plusDays(1).dayLabel(now.toLocalDate()))
        assertEquals("Thu 15", now.toLocalDate().plusDays(2).dayLabel(now.toLocalDate()))
        assertEquals("Tuesday, Oct 13 · 18:00", now.dateLine())
        assertEquals("0.5 km · 6 min walk", distanceLabel(500, 6))
        assertEquals("Music", Category.Music.displayLabel)
        assertEquals("Night", TimeSlot.Night.displayLabel)
        assertEquals("All", CategoryOptions.first().label)
        // El nombre en español sigue igual: lo usan la búsqueda y la IA.
        assertEquals("Música", Category.Music.label)

        val free = listOf(testEvent(start = now, price = null).copy(isFree = true, url = null)).withDistanceFrom(PuertaDelSol).single().toUi(now)
        assertEquals("Free", free.priceLabel)
        assertTrue(free.overline.startsWith("${Category.Music.displayLabel} · ") || free.overline.contains(" · "))
        val paid = listOf(testEvent(start = now, price = "8 €").copy(isFree = false, url = null)).withDistanceFrom(PuertaDelSol).single().toUi(now)
        assertTrue("tickets" in paid.ticketsUrl!! && "hl=en" in paid.ticketsUrl!!)
    }

    @Test fun `la busqueda sin IA entiende ingles`() {
        val tonight = localSearchIntent("Something free tonight")
        assertTrue(tonight.freeOnly)
        assertEquals(TimeSlot.Night, tonight.timeSlot)
        assertTrue(tonight.todayOnly)
        val music = localSearchIntent("Live music nearby")
        assertTrue(Category.Music in music.categories)
        assertEquals(15, music.maxWalkMinutes)
        assertTrue(Category.Markets in localSearchIntent("A market on the weekend").categories)
        assertTrue(localSearchIntent("concert right now").liveNow)
    }

    @Test fun `cualquier otro idioma cae en espanol`() {
        assertEquals(AppLanguage.EN, AppLanguage.of("en-GB"))
        assertEquals(AppLanguage.ES, AppLanguage.of("es-CO"))
        assertEquals(AppLanguage.ES, AppLanguage.of("fr"))
    }
}
