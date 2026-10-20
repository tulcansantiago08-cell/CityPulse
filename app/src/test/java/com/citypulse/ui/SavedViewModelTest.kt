package com.citypulse.ui

import app.cash.turbine.test
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.ui.saved.SavedViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class SavedViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `los guardados que ya terminaron van aparte y se pueden quitar de una vez`() = runTest {
        val today = LocalDate.now()
        val saved = FakeSaved()
        val events = SampleData.events(today)
        saved.save(events.first { it.id == "jazz" }) // hoy 21:00
        saved.save(events.first { it.id == "yoga" }.copy(id = "yoga-ayer", start = today.minusDays(1).atTime(8, 0), end = today.minusDays(1).atTime(9, 0)))
        val vm = SavedViewModel(saved, ObserveCityEvents(FakePreferences(), FakeEventRepository(), FakeLocation()), FixedClock(today.atTime(12, 0)))
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("jazz"), state.events.map { it.id })
            assertEquals(listOf("yoga-ayer"), state.past.map { it.id })
            vm.clearPast()
            assertEquals(emptyList<String>(), expectMostRecentItem().past.map { it.id })
        }
    }
}
