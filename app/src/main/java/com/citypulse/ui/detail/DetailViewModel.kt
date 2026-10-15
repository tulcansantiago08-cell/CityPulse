package com.citypulse.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.ToggleSavedEvent
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import com.citypulse.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = true,
    val event: EventUi? = null,
    val saved: Boolean = false,
    /** Punto desde el que se mide la distancia (usuario o centro de la ciudad). */
    val origin: GeoPoint? = null,
    val usingUserLocation: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCityEvents: ObserveCityEvents,
    eventRepository: EventRepository,
    savedEvents: SavedEventsRepository,
    private val toggleSavedEvent: ToggleSavedEvent,
    clock: Clock,
) : ViewModel() {

    private val eventId = savedStateHandle.toRoute<DetailRoute>().eventId

    /** El evento puede no estar en la ciudad actual (p. ej. uno guardado de otra ciudad). */
    private val storedEvent = flow {
        emit(eventRepository.getEvent(eventId) ?: savedEvents.observeSaved().first().firstOrNull { it.id == eventId })
    }

    val uiState: StateFlow<DetailUiState> = combine(
        observeCityEvents(),
        storedEvent,
        savedEvents.observeSavedIds(),
        clock.ticks(),
    ) { cityEvents, stored, savedIds, now ->
        val nearby = cityEvents?.events?.firstOrNull { it.event.id == eventId }
            ?: stored?.let { listOf(it).withDistanceFrom(cityEvents?.origin ?: it.location).first() }
        DetailUiState(
            loading = false,
            event = nearby?.toUi(now),
            saved = eventId in savedIds,
            origin = cityEvents?.origin,
            usingUserLocation = cityEvents?.usingUserLocation == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleSaved() {
        viewModelScope.launch { toggleSavedEvent(eventId) }
    }
}
