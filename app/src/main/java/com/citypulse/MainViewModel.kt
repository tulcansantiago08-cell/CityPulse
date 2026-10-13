package com.citypulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.repository.ConnectivityRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.ui.navigation.HomeRoute
import com.citypulse.ui.navigation.OnboardingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    connectivity: ConnectivityRepository,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> =
        connectivity.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)


    /** Destino inicial: Inicio si ya hay ciudad elegida, onboarding si no. `null` mientras se lee. */
    val startDestination: StateFlow<Any?> = flow {
        val prefs = preferences.preferences.first()
        emit(if (prefs.onboardingDone && prefs.city != null) HomeRoute else OnboardingRoute)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
