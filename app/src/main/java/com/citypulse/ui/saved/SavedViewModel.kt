package com.citypulse.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val loading: Boolean = true,
    val events: List<EventUi> = emptyList(),
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val savedEvents: SavedEventsRepository,
    observeCityEvents: ObserveCityEvents,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<SavedUiState> = combine(
        savedEvents.observeSaved(),
        observeCityEvents(),
        clock.ticks(),
    ) { saved, cityEvents, now ->
        val events = saved.map { event ->
            // Si el evento no es de la ciudad actual no hay distancia útil: se mide desde el propio evento.
            listOf(event).withDistanceFrom(cityEvents?.origin ?: event.location).first().toUi(now)
        }
        SavedUiState(loading = false, events = events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun remove(eventId: String) {
        viewModelScope.launch { savedEvents.remove(eventId) }
    }
}
