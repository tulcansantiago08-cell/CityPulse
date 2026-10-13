package com.citypulse.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.timeSlotFor
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.nearbyInSlot
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowUiState(
    val loading: Boolean = true,
    val locationLabel: String = "",
    val slot: TimeSlot = TimeSlot.Morning,
    val radiusMinutes: Int = UserPreferences.DEFAULT_RADIUS_MINUTES,
    val events: List<EventUi> = emptyList(),
) {
    val canWiden: Boolean get() = radiusMinutes < UserPreferences.MAX_RADIUS_MINUTES

    val countLabel: String
        get() = "${events.size} ${if (events.size == 1) "evento" else "eventos"} a menos de $radiusMinutes min · ordenados por cercanía"
}

@HiltViewModel
class NowViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val preferences: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    /** Al abrir la pantalla se preselecciona la franja actual. */
    private val selectedSlot = MutableStateFlow(timeSlotFor(clock.now().toLocalTime()))

    val uiState: StateFlow<NowUiState> = combine(
        observeCityEvents(),
        preferences.preferences,
        selectedSlot,
        clock.ticks(),
    ) { cityEvents, prefs, slot, now ->
        val radius = prefs.radiusMinutes
        NowUiState(
            loading = cityEvents == null,
            locationLabel = when {
                cityEvents == null -> ""
                cityEvents.usingUserLocation -> "Tu ubicación, ${cityEvents.city.name}"
                else -> "Centro de ${cityEvents.city.name}"
            },
            slot = slot,
            radiusMinutes = radius,
            events = cityEvents?.events.orEmpty().nearbyInSlot(now, slot, radius).map { it.toUi(now) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowUiState(slot = selectedSlot.value))

    fun selectSlot(slot: TimeSlot) {
        selectedSlot.value = slot
    }

    fun setRadius(minutes: Int) {
        val clamped = minutes.coerceIn(UserPreferences.MIN_RADIUS_MINUTES, UserPreferences.MAX_RADIUS_MINUTES)
        viewModelScope.launch { preferences.setRadiusMinutes(clamped) }
    }

    fun widenRadius() = setRadius(UserPreferences.MAX_RADIUS_MINUTES)
}
