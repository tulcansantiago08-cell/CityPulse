package com.citypulse.ui.directions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.RouteRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.usecase.ARRIVED_METERS
import com.citypulse.domain.usecase.REROUTE_METERS
import com.citypulse.domain.usecase.straightLineRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Destino de la ruta. */
data class Destination(
    val title: String,
    val venue: String,
    val category: Category,
    val location: GeoPoint,
)

data class DirectionsUiState(
    val loading: Boolean = true,
    /** `null` si el evento ya no existe. */
    val destination: Destination? = null,
    /** Posición del usuario en vivo; `null` sin permiso de ubicación o mientras se obtiene. */
    val user: GeoPoint? = null,
    val locationUnavailable: Boolean = false,
    /** Ruta por las calles (o la línea recta si no se pudo calcular). */
    val route: WalkingRoute? = null,
    val calculating: Boolean = false,
    /** Está a menos de [ARRIVED_METERS] del destino. */
    val arrived: Boolean = false,
)

/**
 * Ruta a pie hasta un evento con la ubicación del usuario en vivo. La ruta se calcula al entrar
 * y se recalcula si el usuario se aleja más de [REROUTE_METERS] del punto de partida.
 */
@HiltViewModel
class DirectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val events: EventRepository,
    private val savedEvents: SavedEventsRepository,
    private val location: LocationRepository,
    private val routes: RouteRepository,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle.get<String>("eventId"))

    private val _uiState = MutableStateFlow(DirectionsUiState())
    val uiState: StateFlow<DirectionsUiState> = _uiState.asStateFlow()

    /** Punto desde el que se calculó la ruta actual. */
    private var routedFrom: GeoPoint? = null

    init {
        viewModelScope.launch {
            val event = findEvent()
            if (event == null) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }
            val destination = Destination(event.title, event.venue, event.category, event.location)
            _uiState.update { it.copy(loading = false, destination = destination) }
            location.observePreciseLocation().collect { user -> onLocation(user, destination.location) }
        }
    }

    private suspend fun findEvent(): Event? =
        events.getEvent(eventId) ?: savedEvents.observeSaved().first().firstOrNull { it.id == eventId }

    private suspend fun onLocation(user: GeoPoint?, destination: GeoPoint) {
        if (user == null) {
            _uiState.update { it.copy(user = null, locationUnavailable = true) }
            return
        }
        _uiState.update {
            it.copy(user = user, locationUnavailable = false, arrived = distanceMeters(user, destination) <= ARRIVED_METERS)
        }
        val from = routedFrom
        // Si la última vez solo hubo línea recta (sin conexión), se vuelve a intentar en cada actualización.
        val routeIsExact = _uiState.value.route?.exact == true
        if (from != null && routeIsExact && distanceMeters(from, user) <= REROUTE_METERS) return
        routedFrom = user
        _uiState.update { it.copy(calculating = true, route = it.route ?: straightLineRoute(user, destination)) }
        val route = routes.walkingRoute(user, destination).getOrElse { straightLineRoute(user, destination) }
        _uiState.update { it.copy(route = route, calculating = false) }
    }
}
