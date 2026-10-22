package com.citypulse.ui.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.City
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.citypulse.domain.text.normalizedForSearch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CityUiState(
    val query: String = "",
    val cities: List<City> = emptyList(),
    val selected: City? = null,
    val locating: Boolean = false,
    val locationError: Boolean = false,
    val permissionDenied: Boolean = false,
    /** Se están buscando lugares fuera de la lista de ciudades populares. */
    val searchingPlaces: Boolean = false,
)

@HiltViewModel
class CityViewModel @Inject constructor(
    private val cityRepository: CityRepository,
    private val locationRepository: LocationRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val locating = MutableStateFlow(false)
    private val locationError = MutableStateFlow(false)
    private val permissionDenied = MutableStateFlow(false)

    /** Se emite cuando ya hay ciudad elegida y hay que volver a Inicio. */
    private val doneChannel = Channel<Unit>(Channel.BUFFERED)
    val done = doneChannel.receiveAsFlow()

    private val searchingPlaces = MutableStateFlow(false)

    /**
     * Cualquier lugar del mundo: además de las ciudades populares que coinciden, se busca el nombre
     * con el Geocoder (cuando se deja de escribir) y se añaden los lugares que no estaban.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val places = query
        .debounce(PLACE_SEARCH_DEBOUNCE_MS)
        .mapLatest { q ->
            if (q.trim().length < MIN_PLACE_QUERY) return@mapLatest emptyList()
            searchingPlaces.value = true
            try {
                locationRepository.searchPlaces(q)
            } finally {
                searchingPlaces.value = false
            }
        }
        .onStart { emit(emptyList()) }

    private val selection = combine(query, places) { q, found ->
        val popular = cityRepository.search(q)
        val known = popular.map { it.name.normalizedForSearch() }.toSet()
        q to popular + found.filter { it.name.normalizedForSearch() !in known }
    }

    val uiState: StateFlow<CityUiState> = combine(
        selection, preferences.preferences, locating, locationError, combine(permissionDenied, searchingPlaces, ::Pair),
    ) { (q, cities), prefs, loc, err, (denied, searching) ->
        CityUiState(q, cities, prefs.city, loc, err, denied, searching)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CityUiState(cities = cityRepository.popularCities()))

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun select(city: City) {
        viewModelScope.launch {
            preferences.setCity(city)
            preferences.setOnboardingDone()
            doneChannel.send(Unit)
        }
    }

    /** Resultado del diálogo de permiso lanzado desde "Usar mi ubicación actual". */
    fun onPermissionResult(granted: Boolean) {
        permissionDenied.value = !granted
        if (granted) useCurrentLocation()
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            locating.value = true
            locationError.value = false
            permissionDenied.value = false
            val city = locationRepository.currentLocation()?.let { locationRepository.cityAt(it) }
            locating.value = false
            if (city != null) select(city) else locationError.value = true
        }
    }
}

private const val PLACE_SEARCH_DEBOUNCE_MS = 400L
private const val MIN_PLACE_QUERY = 3
