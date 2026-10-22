package com.citypulse.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.CityEvents
import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.repository.AiQuotaExceededException
import com.citypulse.domain.repository.EventAssistant
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.now
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.localSearchIntent
import com.citypulse.domain.usecase.matching
import com.citypulse.domain.usecase.matchingText
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiSearchStatus { Idle, Thinking, Done }

data class AiSearchUiState(
    val query: String = "",
    val cityName: String = "",
    val status: AiSearchStatus = AiSearchStatus.Idle,
    /** Filtros que entendió la IA (null si se usó la búsqueda de respaldo). */
    val intent: SearchIntent? = null,
    val currency: String = "EUR",
    val usedFallback: Boolean = false,
    /** Se muestran resultados básicos mientras Gemini afina la búsqueda. */
    val refining: Boolean = false,
    /** Los resultados mostrados ya son los de la IA. */
    val aiRefined: Boolean = false,
    /** La IA no respondió porque se agotó la cuota gratuita del día. */
    val quotaExceeded: Boolean = false,
    val results: List<EventUi> = emptyList(),
)

@HiltViewModel
class AiSearchViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val assistant: EventAssistant,
    private val clock: Clock,
) : ViewModel() {

    private val cityEvents: StateFlow<CityEvents?> =
        observeCityEvents().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _state = MutableStateFlow(AiSearchUiState())
    val uiState: StateFlow<AiSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val city = cityEvents.filterNotNull().first().city
            _state.update { it.copy(cityName = city.name, currency = city.currency) }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun search(text: String = _state.value.query) {
        val query = text.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(query = query, status = AiSearchStatus.Thinking) }
            val events = cityEvents.filterNotNull().first()
            val currency = events.city.currency
            val now = clock.now(events.city.zone)

            // 1) Al instante: interpretación básica en el teléfono.
            val local = localSearchIntent(query)
            val localResults = if (local.isEmpty) events.events.matchingText(query, now) else events.events.matching(local, now, currency)
            _state.update {
                it.copy(
                    status = AiSearchStatus.Done, intent = local, usedFallback = false, refining = true,
                    aiRefined = false, quotaExceeded = false, results = localResults.map { r -> r.toUi(now) },
                )
            }

            // 2) Cuando responde Gemini, se sustituyen por los resultados afinados.
            assistant.interpret(query, events.city.name, currency, now).fold(
                onSuccess = { intent ->
                    val results = events.events.matching(intent, now, currency)
                    _state.update {
                        it.copy(intent = intent, usedFallback = false, refining = false, aiRefined = true, results = results.map { r -> r.toUi(now) })
                    }
                },
                // Sin IA (sin conexión, cuota agotada, demasiada espera) se queda la búsqueda básica.
                onFailure = { error ->
                    _state.update { it.copy(usedFallback = true, refining = false, quotaExceeded = error is AiQuotaExceededException) }
                },
            )
        }
    }
}
