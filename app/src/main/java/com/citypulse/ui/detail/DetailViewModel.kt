package com.citypulse.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.AiQuotaExceededException
import com.citypulse.domain.repository.EventAssistant
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.localIn
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.ToggleSavedEvent
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import com.citypulse.ui.navigation.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de un texto generado por IA que se pide bajo demanda. */
sealed interface AiText {
    data object Idle : AiText
    data object Loading : AiText
    data class Ready(val text: String) : AiText
    data object Unavailable : AiText
    data object QuotaExceeded : AiText
}

data class DetailUiState(
    val loading: Boolean = true,
    val event: EventUi? = null,
    val saved: Boolean = false,
    /** Punto desde el que se mide la distancia (usuario o centro de la ciudad). */
    val origin: GeoPoint? = null,
    val usingUserLocation: Boolean = false,
    val summary: AiText = AiText.Idle,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeCityEvents: ObserveCityEvents,
    private val eventRepository: EventRepository,
    private val savedEvents: SavedEventsRepository,
    private val assistant: EventAssistant,
    private val toggleSavedEvent: ToggleSavedEvent,
    clock: Clock,
) : ViewModel() {

    private val eventId = savedStateHandle.toRoute<DetailRoute>().eventId
    private val summary = MutableStateFlow<AiText>(AiText.Idle)

    /** El evento puede no estar en la ciudad actual (p. ej. uno guardado de otra ciudad). */
    private val storedEvent = flow {
        emit(eventRepository.getEvent(eventId) ?: savedEvents.observeSaved().first().firstOrNull { it.id == eventId })
    }

    val uiState: StateFlow<DetailUiState> = combine(
        observeCityEvents(),
        storedEvent,
        savedEvents.observeSavedIds(),
        summary,
        clock.ticks(),
    ) { cityEvents, stored, savedIds, summary, instant ->
        val now = instant.localIn(cityEvents?.city?.zone ?: ZoneId.systemDefault())
        val nearby = cityEvents?.events?.firstOrNull { it.event.id == eventId }
            ?: stored?.let { listOf(it).withDistanceFrom(cityEvents?.origin ?: it.location).first() }
        DetailUiState(
            loading = false,
            event = nearby?.toUi(now),
            saved = eventId in savedIds,
            origin = cityEvents?.origin,
            usingUserLocation = cityEvents?.usingUserLocation == true,
            summary = summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun summarize() {
        if (summary.value is AiText.Loading || summary.value is AiText.Ready) return
        viewModelScope.launch {
            summary.value = AiText.Loading
            val event = eventRepository.getEvent(eventId) ?: savedEvents.observeSaved().first().firstOrNull { it.id == eventId }
            val result = event?.let { assistant.summarize(it) }
            summary.value = when {
                result == null -> AiText.Unavailable
                result.isSuccess -> AiText.Ready(result.getOrThrow())
                result.exceptionOrNull() is AiQuotaExceededException -> AiText.QuotaExceeded
                else -> AiText.Unavailable
            }
        }
    }

    fun toggleSaved() {
        viewModelScope.launch { toggleSavedEvent(eventId) }
    }
}
