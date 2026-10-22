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
import org.junit.Assert.assertNull
import com.citypulse.domain.model.BudgetScale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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
    fun `en curso se calcula con la hora de la ciudad y no con la del telefono`() = runTest {
        // 14:30 en Bogotá son las 21:30 en Madrid (octubre: UTC-5 frente a UTC+2).
        val fromBogota = FixedClock(now.toLocalDate().atTime(14, 30), ZoneId.of("America/Bogota"))
        val vm = HomeViewModel(
            ObserveCityEvents(prefs, events, FakeLocation()),
            events, prefs, saved, ToggleSavedEvent(events, saved), fromBogota,
        )
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(Greeting.Evening, state.greeting)
            assertTrue(state.happeningNow.first { it.id == "jazz" }.live)
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
    fun `presupuesto de 20 euros deja fuera lo mas caro y se guarda`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            val before = expectMostRecentItem()
            assertTrue("flamenco" in before.happeningNow.map { it.id })
            vm.selectBudgetStep(4) // 0, 5, 10, 15, 20 €
            val state = expectMostRecentItem()
            assertEquals(4, prefs.state.value.budgetStep)
            assertEquals("EUR", state.budgetScale.currency)
            val ids = state.happeningNow.map { it.id }
            // Jazz (15 €) y los gratuitos entran; el flamenco (25 €) no.
            assertTrue("jazz" in ids)
            assertFalse("flamenco" in ids)
            assertTrue(state.filtersActive)
            // Cada paso dice cuántos eventos se verían; más presupuesto nunca muestra menos.
            assertEquals(state.happeningNow.size + state.thisWeek.size, state.budgetCounts[4])
            assertEquals(state.budgetCounts.sorted(), state.budgetCounts)
        }
    }

    @Test
    fun `arrastrar el deslizador filtra al momento sin guardar`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.previewBudgetStep(0)
            val state = expectMostRecentItem()
            assertTrue(state.happeningNow.all { it.free })
            assertEquals(0, state.budgetStep)
            assertNull(prefs.state.value.budgetStep)
        }
    }

    @Test
    fun `el ultimo paso del deslizador quita el limite`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.selectBudgetStep(2)
            expectMostRecentItem()
            vm.selectBudgetStep(BudgetScale.forCurrency("EUR").unlimitedStep)
            val state = expectMostRecentItem()
            assertNull(prefs.state.value.budgetStep)
            assertFalse(state.filtersActive)
        }
    }

    @Test
    fun `quitar filtros restablece categoria, gratis y precio`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.selectCategory(Category.Music)
            vm.toggleFreeOnly()
            vm.selectBudgetStep(0)
            assertTrue(expectMostRecentItem().filtersActive)
            vm.clearFilters()
            val state = expectMostRecentItem()
            assertFalse(state.filtersActive)
            assertNull(prefs.state.value.budgetStep)
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
