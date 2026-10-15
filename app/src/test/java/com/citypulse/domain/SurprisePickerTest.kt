package com.citypulse.domain

import com.citypulse.domain.model.SurpriseRolls
import com.citypulse.domain.usecase.pickSurprise
import com.citypulse.domain.usecase.surpriseCandidates
import com.citypulse.domain.usecase.withDistanceFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class SurprisePickerTest {

    private val now = LocalDateTime.of(2026, 10, 13, 19, 0)

    @Test
    fun `candidatos son eventos activos o en las proximas 3 horas`() {
        val events = listOf(
            testEvent("live", start = now.minusHours(1)),
            testEvent("in2h", start = now.plusHours(2)),
            testEvent("in5h", start = now.plusHours(5)),
            testEvent("ended", start = now.minusHours(4), end = now.minusHours(2)),
        ).withDistanceFrom(PuertaDelSol)

        assertEquals(setOf("live", "in2h"), events.surpriseCandidates(now).map { it.event.id }.toSet())
    }

    @Test
    fun `no repite eventos ya vistos mientras queden otros`() {
        val candidates = listOf("a", "b", "c").map { testEvent(it, start = now) }.withDistanceFrom(PuertaDelSol)
        repeat(20) { seed ->
            val pick = pickSurprise(candidates, seenIds = setOf("a", "b"), currentId = "b", random = Random(seed))
            assertEquals("c", pick?.event?.id)
        }
    }

    @Test
    fun `si ya se vieron todos evita repetir el actual`() {
        val candidates = listOf("a", "b").map { testEvent(it, start = now) }.withDistanceFrom(PuertaDelSol)
        repeat(20) { seed ->
            val pick = pickSurprise(candidates, seenIds = setOf("a", "b"), currentId = "a", random = Random(seed))
            assertEquals("b", pick?.event?.id)
        }
        assertNull(pickSurprise(emptyList(), emptySet(), null))
    }

    @Test
    fun `agotados los cercanos amplia al resto del dia antes de repetir`() {
        val soon = listOf("a", "b").map { testEvent(it, start = now.plusHours(1)) }.withDistanceFrom(PuertaDelSol)
        val later = listOf(testEvent("tarde", start = now.plusHours(5))).withDistanceFrom(PuertaDelSol)
        val pick = pickSurprise(soon, seenIds = setOf("a", "b"), currentId = "b", fallback = soon + later)
        assertEquals("tarde", pick?.event?.id)
    }

    @Test
    fun `las tiradas se agotan y se reinician al cambiar de dia`() {
        val today = LocalDate.of(2026, 10, 13)
        var rolls = SurpriseRolls(today)
        repeat(5) { rolls = rolls.afterPicking("e$it") }
        assertFalse(rolls.canRoll(max = 5))
        assertEquals(0, rolls.remaining(max = 5))

        val tomorrow = rolls.on(today.plusDays(1))
        assertTrue(tomorrow.canRoll(max = 5))
        assertEquals(0, tomorrow.used)
        assertTrue(tomorrow.seenEventIds.isEmpty())
    }
}
