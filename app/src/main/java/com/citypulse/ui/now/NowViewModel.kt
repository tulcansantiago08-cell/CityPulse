package com.citypulse.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.RefreshState
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.localIn
import com.citypulse.domain.time.timeSlotFor
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.nearbyInSlot
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NowUiState(
    val loading: Boolean = true,
    val cityName: String = "",
    val usingUserLocation: Boolean = false,
    val slot: TimeSlot = TimeSlot.Morning,
    val radiusMinutes: Int = UserPreferences.DEFAULT_RADIUS_MINUTES,
    val events: List<EventUi> = emptyList(),
) {
    val canWiden: Boolean get() = radiusMinutes < UserPreferences.MAX_RADIUS_MINUTES
}

@HiltViewModel
class NowViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    events: EventRepository,
    private val preferences: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    /** `null` hasta que el usuario elige: entonces se usa la franja actual de la ciudad. */
    private val selectedSlot = MutableStateFlow<TimeSlot?>(null)

    val uiState: StateFlow<NowUiState> = combine(
        observeCityEvents(),
        preferences.preferences,
        selectedSlot,
        events.refreshState,
        clock.ticks(),
    ) { cityEvents, prefs, selected, refresh, instant ->
        val now = instant.localIn(cityEvents?.city?.zone ?: prefs.city?.zone ?: ZoneId.systemDefault())
        val slot = selected ?: timeSlotFor(now.toLocalTime())
        val radius = prefs.radiusMinutes
        NowUiState(
            loading = cityEvents == null || (cityEvents.events.isEmpty() && refresh == RefreshState.Loading),
            cityName = cityEvents?.city?.name.orEmpty(),
            usingUserLocation = cityEvents?.usingUserLocation == true,
            slot = slot,
            radiusMinutes = radius,
            events = cityEvents?.events.orEmpty().nearbyInSlot(now, slot, radius).map { it.toUi(now) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowUiState())

    fun selectSlot(slot: TimeSlot) {
        selectedSlot.value = slot
    }

    fun setRadius(minutes: Int) {
        val clamped = minutes.coerceIn(UserPreferences.MIN_RADIUS_MINUTES, UserPreferences.MAX_RADIUS_MINUTES)
        viewModelScope.launch { preferences.setRadiusMinutes(clamped) }
    }

    fun widenRadius() = setRadius(UserPreferences.MAX_RADIUS_MINUTES)
}
