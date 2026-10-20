package com.citypulse.ui.surprise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.CityEvents
import com.citypulse.domain.model.SurpriseRolls
import com.citypulse.domain.repository.EventAssistant
import com.citypulse.domain.repository.SurpriseRollsRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.localIn
import com.citypulse.domain.time.now
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.pickSurprise
import com.citypulse.domain.usecase.surpriseCandidates
import com.citypulse.domain.usecase.surpriseCandidatesLaterToday
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SurpriseUiState(
    val loading: Boolean = true,
    val event: EventUi? = null,
    val rollNumber: Int = 0,
    val maxRolls: Int = SurpriseRolls.DEFAULT_MAX_PER_DAY,
    /** Frase de la IA sobre por qué se sugiere (null mientras llega o si no hay IA). */
    val explanation: String? = null,
) {
    val rollsLeft: Int get() = (maxRolls - rollNumber).coerceAtLeast(0)
}

@HiltViewModel
class SurpriseViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val rollsRepository: SurpriseRollsRepository,
    private val clock: Clock,
    private val assistant: EventAssistant,
) : ViewModel() {

    /** Explicaciones ya generadas, por evento (una petición a la IA por sorpresa). */
    private val explanations = MutableStateFlow<Map<String, String>>(emptyMap())

    private val maxRolls = SurpriseRolls.DEFAULT_MAX_PER_DAY
    private val rollMutex = Mutex()

    private val cityEvents: StateFlow<CityEvents?> =
        observeCityEvents().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rolls: StateFlow<SurpriseRolls> =
        rollsRepository.observe(clock.now().toLocalDate())
            .stateIn(viewModelScope, SharingStarted.Eagerly, SurpriseRolls(clock.now().toLocalDate()))

    val uiState: StateFlow<SurpriseUiState> = combine(cityEvents, rolls, explanations, clock.ticks()) { events, rolls, texts, instant ->
        val now = instant.localIn(events?.city?.zone ?: ZoneId.systemDefault())
        val current = events?.events?.firstOrNull { it.event.id == rolls.currentEventId && !it.event.hasEnded(now) }
        SurpriseUiState(
            loading = events == null,
            event = current?.toUi(now),
            rollNumber = rolls.used,
            maxRolls = maxRolls,
            explanation = current?.let { texts[it.event.id] },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurpriseUiState())

    init {
        // Si no hay sorpresa vigente (primera vez hoy o la anterior ya terminó), se tira una.
        viewModelScope.launch {
            combine(cityEvents.filterNotNull(), rolls) { events, rolls -> events to rolls }.collect { (events, rolls) ->
                val now = clock.now(events.city.zone)
                val currentValid = events.events.any { it.event.id == rolls.currentEventId && !it.event.hasEnded(now) }
                if (!currentValid && rolls.canRoll(maxRolls)) roll()
            }
        }
    }

    init {
        // Cada vez que cambia la sorpresa, se pide (una sola vez) su explicación a la IA.
        viewModelScope.launch {
            combine(cityEvents.filterNotNull(), rolls) { events, rolls ->
                events.events.firstOrNull { it.event.id == rolls.currentEventId }?.let { it to events.city }
            }.filterNotNull().distinctUntilChangedBy { it.first.event.id }.collect { (event, city) ->
                if (event.event.id !in explanations.value) {
                    assistant.explainSurprise(event, clock.now(city.zone)).onSuccess { text ->
                        explanations.update { it + (event.event.id to text) }
                    }
                }
            }
        }
    }

    /** Botón "Otra": sugiere otro evento si quedan tiradas hoy. */
    fun reroll() {
        viewModelScope.launch { roll() }
    }

    private suspend fun roll() {
        rollMutex.withLock {
            val events = cityEvents.value ?: return
            val current = rolls.value
            if (!current.canRoll(maxRolls)) return
            val now = clock.now(events.city.zone)
            val pick = pickSurprise(
                events.events.surpriseCandidates(now),
                current.seenEventIds,
                current.currentEventId,
                fallback = events.events.surpriseCandidatesLaterToday(now),
            ) ?: return
            rollsRepository.save(current.afterPicking(pick.event.id))
        }
    }
}
