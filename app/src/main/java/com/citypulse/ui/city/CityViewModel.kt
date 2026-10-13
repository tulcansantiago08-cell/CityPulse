package com.citypulse.ui.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.City
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    /** Se emite cuando ya hay ciudad elegida y hay que volver a Inicio. */
    private val doneChannel = Channel<Unit>(Channel.BUFFERED)
    val done = doneChannel.receiveAsFlow()

    val uiState: StateFlow<CityUiState> = combine(query, preferences.preferences, locating, locationError) { q, prefs, loc, err ->
        CityUiState(q, cityRepository.search(q), prefs.city, loc, err)
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

    fun useCurrentLocation() {
        viewModelScope.launch {
            locating.value = true
            locationError.value = false
            val city = locationRepository.currentLocation()?.let { locationRepository.cityAt(it) }
            locating.value = false
            if (city != null) select(city) else locationError.value = true
        }
    }
}
