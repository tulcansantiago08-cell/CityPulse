package com.citypulse.ui

import androidx.lifecycle.SavedStateHandle
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.RouteRepository
import com.citypulse.domain.testEvent
import com.citypulse.ui.directions.DirectionsViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DirectionsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val plaza = GeoPoint(1.2147108, -77.2782704)
    private val taminango = GeoPoint(1.2152858, -77.2842225)
    private val event = testEvent("tam", start = LocalDateTime.of(2026, 11, 9, 8, 0), location = taminango)

    private val location = MovingLocation(plaza)
    private val routes = FakeRoutes()
    private val saved = FakeSaved()

    private suspend fun viewModel(): DirectionsViewModel {
        saved.save(event)
        return DirectionsViewModel(SavedStateHandle(mapOf("eventId" to "tam")), FakeEventRepository(), saved, location, routes)
    }

    @Test
    fun `calcula la ruta por las calles desde la ubicacion actual`() = runTest {
        val state = viewModel().uiState.value
        assertEquals("Evento tam", state.destination?.title)
        assertEquals(plaza, state.user)
        assertEquals(listOf(plaza to taminango), routes.requests)
        assertTrue(state.route!!.exact)
        assertEquals(9, state.route!!.minutes)
        assertFalse(state.calculating)
    }

    @Test
    fun `no recalcula por movimientos pequenos y si al alejarse`() = runTest {
        val vm = viewModel()
        location.point.value = GeoPoint(plaza.latitude + 0.0001, plaza.longitude) // ~11 m
        assertEquals(1, routes.requests.size)
        location.point.value = GeoPoint(plaza.latitude + 0.0010, plaza.longitude) // ~110 m
        assertEquals(2, routes.requests.size)
        assertEquals(location.point.value, vm.uiState.value.user)
    }

    @Test
    fun `sin conexion muestra la linea recta y lo vuelve a intentar`() = runTest {
        routes.fail = true
        val vm = viewModel()
        val route = vm.uiState.value.route!!
        assertFalse(route.exact)
        assertEquals(listOf(plaza, taminango), route.points)
        routes.fail = false
        location.point.value = GeoPoint(plaza.latitude + 0.0001, plaza.longitude)
        assertTrue(vm.uiState.value.route!!.exact)
    }

    @Test
    fun `sin permiso de ubicacion lo indica`() = runTest {
        location.point.value = null
        val state = viewModel().uiState.value
        assertTrue(state.locationUnavailable)
        assertNull(state.route)
        assertTrue(routes.requests.isEmpty())
    }

    @Test
    fun `al llegar al destino lo indica`() = runTest {
        val vm = viewModel()
        location.point.value = GeoPoint(taminango.latitude + 0.0001, taminango.longitude)
        assertTrue(vm.uiState.value.arrived)
    }
}

private class MovingLocation(start: GeoPoint?) : LocationRepository {
    val point = MutableStateFlow(start)
    override suspend fun currentLocation(): GeoPoint? = point.value
    override fun observeLocation(): Flow<GeoPoint?> = point
    override suspend fun cityAt(point: GeoPoint): City = SampleData.Madrid
    override suspend fun searchPlaces(query: String): List<City> = emptyList()
}

private class FakeRoutes : RouteRepository {
    var fail = false
    val requests = mutableListOf<Pair<GeoPoint, GeoPoint>>()
    override suspend fun walkingRoute(from: GeoPoint, to: GeoPoint): Result<WalkingRoute> {
        if (fail) return Result.failure(IllegalStateException("sin red"))
        requests += from to to
        return Result.success(WalkingRoute(listOf(from, GeoPoint(1.215, -77.280), to), 700.0, 9))
    }
}
