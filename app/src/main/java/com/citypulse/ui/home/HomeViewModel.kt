package com.citypulse.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.CityEvents
import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.model.RefreshState
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.model.fitsBudget
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.time.greetingFor
import com.citypulse.domain.time.localIn
import com.citypulse.domain.usecase.ObserveCityEvents
import com.citypulse.domain.usecase.ToggleSavedEvent
import com.citypulse.domain.usecase.happeningNow
import com.citypulse.domain.usecase.matches
import com.citypulse.domain.usecase.upcomingThisWeek
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.dateLine
import com.citypulse.ui.common.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val cityName: String = "",
    val dateLine: String = "",
    val greeting: Greeting = Greeting.Morning,
    val selectedCategory: Category? = null,
    val freeOnly: Boolean = false,
    val happeningNow: List<EventUi> = emptyList(),
    val thisWeek: List<EventUi> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    /** Paso elegido en el deslizador de presupuesto; `null` es sin límite. */
    val budgetStep: Int? = null,
    /** Escala del deslizador en la moneda de la ciudad. */
    val budgetScale: BudgetScale = BudgetScale.forCurrency("EUR"),
    /** Eventos que se verían con cada paso del deslizador (mismo índice que los pasos). */
    val budgetCounts: List<Int> = emptyList(),
    /** Eventos de la semana sin precio publicado: no se pueden comparar con un presupuesto. */
    val unknownPriceCount: Int = 0,
    /** No hay eventos que mostrar porque fallaron las descargas. */
    val error: Boolean = false,
    /** Se están descargando eventos nuevos (indicador de "deslizar para actualizar"). */
    val refreshing: Boolean = false,
) {
    /** Hay algún filtro que puede estar escondiendo eventos. */
    val filtersActive: Boolean get() = freeOnly || selectedCategory != null || budgetStep != null
}

private data class HomeInputs(
    val cityEvents: CityEvents?,
    val prefs: UserPreferences,
    val category: Category?,
    val savedIds: Set<String>,
    val refresh: RefreshState,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeCityEvents: ObserveCityEvents,
    private val events: EventRepository,
    private val preferences: UserPreferencesRepository,
    savedEvents: SavedEventsRepository,
    private val toggleSavedEvent: ToggleSavedEvent,
    clock: Clock,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<Category?>(null)

    /** Paso del deslizador mientras se arrastra, antes de guardarlo. */
    private val budgetDraft = MutableStateFlow<Int?>(null)

    private val inputs = combine(
        observeCityEvents(),
        preferences.preferences,
        selectedCategory,
        savedEvents.observeSavedIds(),
        events.refreshState,
    ) { cityEvents, prefs, category, savedIds, refresh -> HomeInputs(cityEvents, prefs, category, savedIds, refresh) }

    val uiState: StateFlow<HomeUiState> = combine(inputs, clock.ticks(), budgetDraft) { (cityEvents, prefs, category, savedIds, refresh), instant, draft ->
        // "Ahora" en la hora local de la ciudad elegida, no en la del teléfono.
        val now = instant.localIn(cityEvents?.city?.zone ?: prefs.city?.zone ?: ZoneId.systemDefault())
        val nothingCached = cityEvents?.events.isNullOrEmpty()
        val city = cityEvents?.city ?: prefs.city
        val scale = city?.let(BudgetScale::forCity) ?: BudgetScale.forCurrency("EUR")
        val step = draft ?: prefs.budgetStep ?: scale.unlimitedStep
        val base = cityEvents?.events.orEmpty().filter { it.event.matches(prefs.freeOnly, category) }
        fun withinStep(s: Int) = scale.maxFor(s).let { max -> base.filter { it.event.fitsBudget(max, scale.currency) } }
        val filtered = withinStep(step)
        HomeUiState(
            // Sin nada en caché, mientras se descarga se muestran esqueletos en vez de "no hay eventos".
            loading = cityEvents == null || (nothingCached && refresh == RefreshState.Loading),
            error = nothingCached && refresh == RefreshState.Error,
            refreshing = refresh == RefreshState.Loading && !nothingCached,
            cityName = cityEvents?.city?.name ?: prefs.city?.name.orEmpty(),
            dateLine = now.dateLine(),
            greeting = greetingFor(now.toLocalTime()),
            selectedCategory = category,
            freeOnly = prefs.freeOnly,
            happeningNow = filtered.happeningNow(now).map { it.toUi(now) },
            thisWeek = filtered.upcomingThisWeek(now).map { it.toUi(now) },
            savedIds = savedIds,
            budgetStep = step.takeIf { it < scale.unlimitedStep },
            budgetScale = scale,
            // Lo mismo que muestran "Pasando ahora" y "Esta semana" con cada presupuesto.
            budgetCounts = (0..scale.unlimitedStep).map { s ->
                withinStep(s).let { it.happeningNow(now).size + it.upcomingThisWeek(now).size }
            },
            unknownPriceCount = base.let { it.happeningNow(now) + it.upcomingThisWeek(now) }
                .count { !it.event.isFree && (it.event.priceMin == null || it.event.currency != scale.currency) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(category: Category?) {
        selectedCategory.value = category
    }

    /** "Deslizar para actualizar": vuelve a pedir los eventos aunque la caché sea reciente. */
    fun refresh() = retry()

    fun retry() {
        viewModelScope.launch { preferences.preferences.first().city?.let { events.refresh(it) } }
    }

    fun toggleSaved(eventId: String) {
        viewModelScope.launch { toggleSavedEvent(eventId) }
    }

    /** Mientras se arrastra el deslizador: filtra al momento, sin guardar todavía. */
    fun previewBudgetStep(step: Int) {
        budgetDraft.value = step
    }

    /** Paso del deslizador de presupuesto al soltarlo; el último ("Sin límite") quita el filtro. */
    fun selectBudgetStep(step: Int) {
        val value = step.takeIf { it < uiState.value.budgetScale.unlimitedStep }
        budgetDraft.value = step
        viewModelScope.launch {
            preferences.setBudgetStep(value)
            budgetDraft.value = null
        }
    }

    fun clearFilters() {
        selectedCategory.value = null
        viewModelScope.launch {
            preferences.setFreeOnly(false)
            preferences.setBudgetStep(null)
            budgetDraft.value = null
        }
    }

    fun toggleFreeOnly() {
        viewModelScope.launch { preferences.setFreeOnly(!uiState.value.freeOnly) }
    }
}
