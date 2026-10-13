package com.citypulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.Category
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.time.greetingFor
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.happeningNow
import com.citypulse.domain.usecase.matches
import com.citypulse.domain.usecase.upcomingThisWeek
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.dateLine
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val cityName: String = "",
    val dateLine: String = "",
    val greeting: Greeting = Greeting.Morning,
    val selectedCategory: Category? = null,
    val freeOnly: Boolean = false,
    val happeningNow: List<EventUi> = emptyList(),
    val thisWeek: List<EventUi> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val preferences: UserPreferencesRepository,
    clock: Clock,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        observeCityEvents(),
        preferences.preferences,
        selectedCategory,
        clock.ticks(),
    ) { cityEvents, prefs, category, now ->
        val filtered = cityEvents?.events.orEmpty().filter { it.event.matches(prefs.freeOnly, category) }
        HomeUiState(
            loading = cityEvents == null,
            cityName = cityEvents?.city?.name ?: prefs.city?.name.orEmpty(),
            dateLine = now.dateLine(),
            greeting = greetingFor(now.toLocalTime()),
            selectedCategory = category,
            freeOnly = prefs.freeOnly,
            happeningNow = filtered.happeningNow(now).map { it.toUi(now) },
            thisWeek = filtered.upcomingThisWeek(now).map { it.toUi(now) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(category: Category?) {
        selectedCategory.value = category
    }

    fun toggleFreeOnly() {
        viewModelScope.launch { preferences.setFreeOnly(!uiState.value.freeOnly) }
    }
}
