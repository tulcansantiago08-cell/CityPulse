package com.citypulse.ui.surprise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.CityEvents
import com.citypulse.domain.model.SurpriseRolls
import com.citypulse.domain.repository.SurpriseRollsRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.pickSurprise
import com.citypulse.domain.usecase.surpriseCandidates
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class SurpriseUiState(
    val loading: Boolean = true,
    val event: EventUi? = null,
    val rollNumber: Int = 0,
    val maxRolls: Int = SurpriseRolls.DEFAULT_MAX_PER_DAY,
) {
    val rollsLeft: Int get() = (maxRolls - rollNumber).coerceAtLeast(0)
}

@HiltViewModel
class SurpriseViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val rollsRepository: SurpriseRollsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val maxRolls = SurpriseRolls.DEFAULT_MAX_PER_DAY
    private val rollMutex = Mutex()

    private val cityEvents: StateFlow<CityEvents?> =
        observeCityEvents().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rolls: StateFlow<SurpriseRolls> =
        rollsRepository.observe(clock.now().toLocalDate())
            .stateIn(viewModelScope, SharingStarted.Eagerly, SurpriseRolls(clock.now().toLocalDate()))

    val uiState: StateFlow<SurpriseUiState> = combine(cityEvents, rolls, clock.ticks()) { events, rolls, now ->
        val current = events?.events?.firstOrNull { it.event.id == rolls.currentEventId && !it.event.hasEnded(now) }
        SurpriseUiState(
            loading = events == null,
            event = current?.toUi(now),
            rollNumber = rolls.used,
            maxRolls = maxRolls,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurpriseUiState())

    init {
        // Si no hay sorpresa vigente (primera vez hoy o la anterior ya terminó), se tira una.
        viewModelScope.launch {
            combine(cityEvents.filterNotNull(), rolls) { events, rolls -> events to rolls }.collect { (events, rolls) ->
                val now = clock.now()
                val currentValid = events.events.any { it.event.id == rolls.currentEventId && !it.event.hasEnded(now) }
                if (!currentValid && rolls.canRoll(maxRolls)) roll()
            }
        }
    }

    /** Botón "Otra": sugiere otro evento si quedan tiradas hoy. */
    fun reroll() {
        viewModelScope.launch { roll() }
    }

    private suspend fun roll() = rollMutex.withLock {
        val events = cityEvents.value ?: return
        val current = rolls.value
        if (!current.canRoll(maxRolls)) return
        val now = clock.now()
        val pick = pickSurprise(events.events.surpriseCandidates(now), current.seenEventIds, current.currentEventId) ?: return
        rollsRepository.save(current.afterPicking(pick.event.id))
    }
}
