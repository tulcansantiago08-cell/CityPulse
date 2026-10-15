package com.citypulse.ui

import app.cash.turbine.test
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.domain.model.Category
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.ToggleSavedEvent
import com.citypulse.ui.home.HomeViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    // A las 21:30 de hoy: jazz, slam, dj y tapeo están en curso; el flamenco empieza a las 22:00.
    private val now = LocalDate.now().atTime(21, 30)
    private val events = FakeEventRepository()
    private val prefs = FakePreferences()
    private val saved = FakeSaved()

    private fun viewModel() = HomeViewModel(
        ObserveCityEvents(prefs, events, FakeLocation()),
        events, prefs, saved, ToggleSavedEvent(events, saved), FixedClock(now),
    )

    @Test
    fun `muestra lo que esta pasando ahora y el saludo de la noche`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(Greeting.Evening, state.greeting)
            assertEquals("Madrid", state.cityName)
            assertTrue("jazz" in state.happeningNow.map { it.id })
            assertTrue("flamenco" in state.happeningNow.map { it.id }) // empieza en menos de 1 h
            assertTrue(state.thisWeek.none { it.id in state.happeningNow.map { e -> e.id } })
        }
    }

    @Test
    fun `solo gratis oculta los eventos de pago y se guarda en preferencias`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.toggleFreeOnly()
            val state = expectMostRecentItem()
            assertTrue(state.freeOnly)
            assertTrue(prefs.state.value.freeOnly)
            assertTrue(state.happeningNow.all { it.free })
            assertFalse("jazz" in state.happeningNow.map { it.id })
        }
    }

    @Test
    fun `filtrar por categoria`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.selectCategory(Category.Food)
            val state = expectMostRecentItem()
            assertEquals(listOf("sanil"), state.happeningNow.map { it.id })
        }
    }

    @Test
    fun `guardar desde una tarjeta marca el corazon`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.toggleSaved("jazz")
            assertTrue("jazz" in expectMostRecentItem().savedIds)
        }
    }
}
