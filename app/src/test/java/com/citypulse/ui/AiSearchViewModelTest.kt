package com.citypulse.ui

import app.cash.turbine.test
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.ui.search.AiSearchStatus
import com.citypulse.ui.search.AiSearchViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AiSearchViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val now = LocalDate.now().atTime(21, 30)
    private val events = FakeEventRepository()
    private val observe = ObserveCityEvents(FakePreferences(), events, FakeLocation())

    @Test
    fun `con ia aplica los filtros que devuelve sobre los eventos reales`() = runTest {
        val assistant = FakeAssistant() // devuelve "solo gratis"
        val vm = AiSearchViewModel(observe, assistant, FixedClock(now))
        vm.uiState.test {
            vm.search("algo sin pagar")
            val state = expectMostRecentItem()
            assertEquals(AiSearchStatus.Done, state.status)
            assertFalse(state.usedFallback)
            assertEquals("algo sin pagar", assistant.lastQuery)
            assertTrue(state.results.isNotEmpty())
            assertTrue(state.results.all { it.free })
        }
    }

    @Test
    fun `sin ia usa la busqueda basica y lo indica`() = runTest {
        val vm = AiSearchViewModel(observe, FakeAssistant(available = false), FixedClock(now))
        vm.uiState.test {
            vm.search("algo gratis esta noche")
            val state = expectMostRecentItem()
            assertTrue(state.usedFallback)
            assertTrue(state.intent!!.freeOnly)
            assertTrue(state.results.isNotEmpty())
            assertTrue(state.results.all { it.free })
        }
    }
}
