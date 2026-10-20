package com.citypulse.ui

import app.cash.turbine.test
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.ui.now.NowViewModel
import com.citypulse.ui.surprise.SurpriseViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class NowAndSurpriseViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val now = LocalDate.now().atTime(21, 30)
    private val events = FakeEventRepository()
    private val prefs = FakePreferences()
    private val observe = ObserveCityEvents(prefs, events, FakeLocation())

    @Test
    fun `ahora preselecciona la franja actual y ordena por cercania dentro del radio`() = runTest {
        NowViewModel(observe, events, prefs, FixedClock(now)).uiState.test {
            val state = expectMostRecentItem()
            assertEquals(TimeSlot.Night, state.slot)
            assertTrue(state.usingUserLocation)
            assertTrue(state.events.all { it.walkMinutes <= 15 })
            assertEquals(state.events.sortedBy { it.walkMinutes }, state.events)
        }
    }

    @Test
    fun `ampliar el radio a 30 min lo guarda y muestra mas eventos`() = runTest {
        val vm = NowViewModel(observe, events, prefs, FixedClock(now))
        vm.uiState.test {
            val before = expectMostRecentItem().events.size
            vm.widenRadius()
            val after = expectMostRecentItem()
            assertEquals(30, prefs.state.value.radiusMinutes)
            assertTrue(after.events.size >= before)
            assertTrue(!after.canWiden)
        }
    }

    @Test
    fun `sorpresa tira al abrir y descuenta tiradas hasta agotarlas`() = runTest {
        val rolls = FakeRolls(now.toLocalDate())
        val vm = SurpriseViewModel(observe, rolls, FixedClock(now), FakeAssistant())
        vm.uiState.test {
            var state = expectMostRecentItem()
            assertNotNull(state.event)
            assertEquals(1, state.rollNumber)
            assertEquals("Ve a ${state.event!!.title}", state.explanation)
            repeat(10) { vm.reroll() }
            state = expectMostRecentItem()
            assertEquals(state.maxRolls, state.rollNumber)
            assertEquals(0, state.rollsLeft)
        }
    }
}
