package com.citypulse.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.nextOccurrences
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MapUiState(
    val loading: Boolean = true,
    val cityName: String = "",
    val origin: GeoPoint? = null,
    val usingUserLocation: Boolean = false,
    val events: List<EventUi> = emptyList(),
    val selectedId: String? = null,
) {
    val selected: EventUi? get() = events.firstOrNull { it.id == selectedId } ?: events.firstOrNull()
}

@HiltViewModel
class MapViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    clock: Clock,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapUiState> = combine(observeCityEvents(), selectedId, clock.ticks()) { cityEvents, selected, now ->
        val limit = now.toLocalDate().plusDays(7).atStartOfDay()
        // En el mapa: lo que no ha terminado y empieza esta semana; primero lo que está en curso.
        val events = cityEvents?.events.orEmpty()
            .filter { !it.event.hasEnded(now) && it.event.start.isBefore(limit) }
            .nextOccurrences()
            .sortedWith(compareByDescending<com.citypulse.domain.model.NearbyEvent> { it.event.isLive(now) }.thenBy { it.walkMinutes })
            .map { it.toUi(now) }
        MapUiState(
            loading = cityEvents == null,
            cityName = cityEvents?.city?.name.orEmpty(),
            origin = cityEvents?.origin,
            usingUserLocation = cityEvents?.usingUserLocation == true,
            events = events,
            selectedId = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun select(eventId: String) {
        selectedId.value = eventId
    }
}
