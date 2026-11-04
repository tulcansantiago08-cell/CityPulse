package com.citypulse.ui

import androidx.lifecycle.SavedStateHandle
import com.citypulse.data.fake.FakeEventRepository
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.NoRouteException
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.RouteRepository
import com.citypulse.domain.testEvent
import com.citypulse.ui.directions.DirectionsViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
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
        assertEquals(TravelMode.WALK, state.selectedMode)
        assertEquals(listOf(Triple(plaza, taminango, TravelMode.WALK)), routes.requests.filter { it.third == TravelMode.WALK })
        assertTrue(state.route!!.exact)
        assertEquals(9, state.route!!.minutes)
        assertFalse(state.calculating)
    }

    @Test
    fun `no recalcula por movimientos pequenos y si al alejarse`() = runTest {
        val vm = viewModel()
        val walking = { routes.requests.count { it.third == TravelMode.WALK } }
        location.point.value = GeoPoint(plaza.latitude + 0.0001, plaza.longitude) // ~11 m
        assertEquals(1, walking())
        location.point.value = GeoPoint(plaza.latitude + 0.0010, plaza.longitude) // ~110 m
        assertEquals(2, walking())
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
    fun `calcula cada modo una vez y no pide ruta de transporte publico`() = runTest {
        val vm = viewModel()
        assertEquals(setOf(TravelMode.WALK, TravelMode.BIKE, TravelMode.CAR), vm.uiState.value.routes.keys)
        assertEquals(3, routes.requests.size)
        vm.selectMode(TravelMode.TRANSIT)
        assertEquals(3, routes.requests.size)
        assertNull(vm.uiState.value.route)
    }

    @Test
    fun `al moverse solo recalcula el modo elegido y los demas al elegirlos`() = runTest {
        val vm = viewModel()
        location.point.value = GeoPoint(plaza.latitude + 0.0010, plaza.longitude)
        assertEquals(1, routes.requests.count { it.third == TravelMode.CAR })
        vm.selectMode(TravelMode.CAR)
        assertEquals(2, routes.requests.count { it.third == TravelMode.CAR })
    }

    @Test
    fun `tras un cambio grande de posicion actualiza tambien los demas modos`() = runTest {
        viewModel()
        location.point.value = GeoPoint(plaza.latitude + 0.05, plaza.longitude) // ~5,5 km
        assertEquals(2, routes.requests.count { it.third == TravelMode.BIKE })
    }

    @Test
    fun `lejos del destino abre en carro y a pie cerca`() = runTest {
        location.point.value = GeoPoint(1.30, -77.28) // ~10 km
        assertEquals(TravelMode.CAR, viewModel().uiState.value.selectedMode)
    }

    @Test
    fun `desde otra ciudad sin camino lo indica y no reintenta en cada actualizacion`() = runTest {
        location.point.value = GeoPoint(40.4169, -3.7035) // Madrid
        routes.noRoute = true
        val vm = viewModel()
        val route = vm.uiState.value.route!!
        assertFalse(route.exact)
        assertFalse(route.reachable)
        val attempts = routes.attempts
        location.point.value = GeoPoint(40.41691, -3.7035)
        assertEquals(attempts, routes.attempts)
    }

    @Test
    fun `al cambiar de ciudad recalcula desde la nueva ubicacion`() = runTest {
        val vm = viewModel()
        val bogota = GeoPoint(4.6, -74.08)
        location.point.value = bogota
        assertEquals(bogota, vm.uiState.value.user)
        assertEquals(bogota, routes.requests.last { it.third == vm.uiState.value.selectedMode }.first)
    }

    @Test
    fun `si la ubicacion cambia durante el calculo recalcula desde la nueva`() = runTest {
        val gate = CompletableDeferred<Unit>()
        routes.gate = gate
        val vm = viewModel()
        val bogota = GeoPoint(4.6, -74.08)
        location.point.value = bogota
        routes.gate = null
        gate.complete(Unit)
        val mode = vm.uiState.value.selectedMode
        assertEquals(bogota, routes.requests.last { it.third == mode }.first)
        assertFalse(vm.uiState.value.calculating)
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
    var noRoute = false
    var attempts = 0
    var gate: CompletableDeferred<Unit>? = null
    val requests = mutableListOf<Triple<GeoPoint, GeoPoint, TravelMode>>()
    override suspend fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): Result<TravelRoute> {
        attempts++
        gate?.await()
        if (noRoute) return Result.failure(NoRouteException())
        if (fail) return Result.failure(IllegalStateException("sin red"))
        requests += Triple(from, to, mode)
        return Result.success(TravelRoute(listOf(from, GeoPoint(1.215, -77.280), to), 700.0, 9))
    }
}
