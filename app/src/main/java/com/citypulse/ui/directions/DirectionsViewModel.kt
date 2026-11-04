package com.citypulse.ui.directions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.NoRouteException
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.RouteRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.usecase.ARRIVED_METERS
import com.citypulse.domain.usecase.MAX_USER_DISTANCE_FROM_CITY_METERS
import com.citypulse.domain.usecase.REFRESH_OTHER_MODES_METERS
import com.citypulse.domain.usecase.REROUTE_METERS
import com.citypulse.domain.usecase.defaultTravelMode
import com.citypulse.domain.usecase.directionsPlaceQuery
import com.citypulse.domain.usecase.straightLineRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    /** Cómo buscar el lugar en Google Maps; `null` si no se sabe la ciudad: se usan las coordenadas. */
    val placeQuery: String? = null,
)

data class DirectionsUiState(
    val loading: Boolean = true,
    /** `null` si el evento ya no existe. */
    val destination: Destination? = null,
    /** Posición del usuario en vivo; `null` sin permiso de ubicación o mientras se obtiene. */
    val user: GeoPoint? = null,
    /** Punto de partida de la ruta: el usuario o, si está muy lejos, el centro de la ciudad del evento. */
    val origin: GeoPoint? = null,
    /** Ciudad cuyo centro es el punto de partida, porque el usuario está lejos; si no, `null`. */
    val exploringCity: String? = null,
    /** Cómo nombrar ese punto de partida en Google Maps: con coordenadas mostraría el negocio más cercano. */
    val exploringOrigin: String? = null,
    /** Zona horaria de la ciudad del evento, la misma de Inicio; `null` si no se conoce. */
    val zone: ZoneId? = null,
    val locationUnavailable: Boolean = false,
    val selectedMode: TravelMode = TravelMode.WALK,
    /** Ruta de cada modo ya consultado (o la línea recta si no se pudo calcular). */
    val routes: Map<TravelMode, TravelRoute> = emptyMap(),
    val calculatingModes: Set<TravelMode> = emptySet(),
    /** Está a menos de [ARRIVED_METERS] del destino. */
    val arrived: Boolean = false,
) {
    val route: TravelRoute? get() = routes[selectedMode]
    val calculating: Boolean get() = selectedMode in calculatingModes
}

/**
 * Rutas hasta un evento desde la ubicación del usuario en vivo, sea cual sea la ciudad donde esté.
 * El modo elegido se recalcula si el usuario se aleja más de [REROUTE_METERS] del punto de partida;
 * los demás se piden para mostrar su tiempo y solo se actualizan al elegirlos o tras un cambio
 * grande de posición: así no se sobrecarga el servidor de rutas mientras el usuario camina.
 */
@HiltViewModel
class DirectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val events: EventRepository,
    private val savedEvents: SavedEventsRepository,
    private val location: LocationRepository,
    private val routes: RouteRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle.get<String>("eventId"))

    private val _uiState = MutableStateFlow(DirectionsUiState())
    val uiState: StateFlow<DirectionsUiState> = _uiState.asStateFlow()

    /** Punto desde el que se calculó la ruta de cada modo. */
    private val routedFrom = mutableMapOf<TravelMode, GeoPoint>()
    private var locationJob: Job? = null
    private var modeChosen = false

    /** Ciudad del evento, para partir de su centro si el usuario está lejos. */
    private var eventCity: City? = null
    private var lastLocation: GeoPoint? = null

    init {
        viewModelScope.launch {
            val event = findEvent()
            if (event == null) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }
            _uiState.update {
                it.copy(loading = false, destination = Destination(event.title, event.venue, event.category, event.location))
            }
            // Normalmente es la ciudad elegida, que se conoce al momento.
            eventCity = preferences.preferences.first().city?.takeIf { it.isCityOf(event.location) }
            _uiState.update { it.copy(zone = eventCity?.zone) }
            retryLocation()
            // Después de empezar la ruta: resolver la ciudad puede tardar y no debe retrasarla.
            val city = runCatching { location.cityAt(event.location) }.getOrNull()
            _uiState.update { state ->
                state.copy(destination = state.destination?.copy(placeQuery = directionsPlaceQuery(event.venue, city?.name)))
            }
            if (eventCity == null && city?.isCityOf(event.location) == true) {
                eventCity = city
                _uiState.update { it.copy(zone = city.zone) }
                lastLocation?.let { onLocation(it, event.location) }
            }
        }
    }

    /** Vuelve a escuchar la ubicación, p. ej. después de conceder el permiso. */
    fun retryLocation() {
        val target = _uiState.value.destination?.location ?: return
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            location.observePreciseLocation().collect { user -> onLocation(user, target) }
        }
    }

    fun selectMode(mode: TravelMode) {
        modeChosen = true
        _uiState.update { it.copy(selectedMode = mode) }
        val state = _uiState.value
        val origin = state.origin ?: return
        val target = state.destination?.location ?: return
        viewModelScope.launch { refresh(mode, origin, target) }
    }

    /** Ciudad con centro cerca del evento. Una desconocida tiene su centro en el propio punto: no sirve de partida. */
    private fun City.isCityOf(point: GeoPoint) =
        distanceMeters(center, point).let { it > ARRIVED_METERS && it <= MAX_USER_DISTANCE_FROM_CITY_METERS }

    private suspend fun findEvent(): Event? =
        events.getEvent(eventId) ?: savedEvents.observeSaved().first().firstOrNull { it.id == eventId }

    private fun onLocation(user: GeoPoint?, destination: GeoPoint) {
        lastLocation = user
        if (user == null) {
            _uiState.update { it.copy(user = null, origin = null, exploringCity = null, exploringOrigin = null, locationUnavailable = true) }
            return
        }
        val exploring = eventCity?.takeIf { distanceMeters(user, destination) > MAX_USER_DISTANCE_FROM_CITY_METERS }
        val origin = exploring?.center ?: user
        _uiState.update {
            it.copy(
                // Lejos, el punto del usuario no se dibuja: el mapa tendría que abarcar medio mundo.
                user = user.takeIf { exploring == null },
                origin = origin,
                exploringCity = exploring?.name,
                exploringOrigin = exploring?.let { city -> listOf(city.name, city.country).filter(String::isNotBlank).joinToString(", ") },
                locationUnavailable = false,
                arrived = distanceMeters(user, destination) <= ARRIVED_METERS,
                selectedMode = if (modeChosen || it.origin != null) it.selectedMode else defaultTravelMode(origin, destination),
            )
        }
        viewModelScope.launch {
            val selected = _uiState.value.selectedMode
            refresh(selected, origin, destination)
            // Sin conexión no se insiste con los demás: se piden cuando el modo elegido vuelva a funcionar.
            val failed = _uiState.value.routes[selected]?.takeIf { selected.routedInApp }
            if (failed != null && !failed.exact && failed.reachable) return@launch
            TravelMode.entries
                .filter { mode -> mode != selected && routedFrom[mode]?.let { distanceMeters(it, origin) > REFRESH_OTHER_MODES_METERS } != false }
                .forEach { refresh(it, origin, destination) }
        }
    }

    private suspend fun refresh(mode: TravelMode, user: GeoPoint, destination: GeoPoint) {
        if (!mode.routedInApp || mode in _uiState.value.calculatingModes || !isStale(mode, user)) return
        routedFrom[mode] = user
        _uiState.update {
            it.copy(
                calculatingModes = it.calculatingModes + mode,
                routes = if (mode in it.routes) it.routes else it.routes + (mode to straightLineRoute(user, destination)),
            )
        }
        val route = routes.route(user, destination, mode).getOrElse { error ->
            straightLineRoute(user, destination, reachable = error !is NoRouteException)
        }
        _uiState.update { it.copy(routes = it.routes + (mode to route), calculatingModes = it.calculatingModes - mode) }
        // Si el usuario se movió durante el cálculo, puede que no llegue otra actualización de posición.
        val latest = _uiState.value.origin
        if (latest != null && latest != user && distanceMeters(user, latest) > REROUTE_METERS) refresh(mode, latest, destination)
    }

    /** Si la última vez solo hubo línea recta por falta de conexión, se vuelve a intentar. */
    private fun isStale(mode: TravelMode, user: GeoPoint): Boolean {
        val from = routedFrom[mode] ?: return true
        val route = _uiState.value.routes[mode] ?: return true
        return (!route.exact && route.reachable) || distanceMeters(from, user) > REROUTE_METERS
    }
}
