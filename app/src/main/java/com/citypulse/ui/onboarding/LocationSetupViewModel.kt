package com.citypulse.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resultado de configurar la ubicación: a dónde debe navegar la app. */
enum class LocationSetupResult { Home, ChooseCity, Denied }

/** Lógica compartida del onboarding y de la pantalla "Sin acceso a tu ubicación". */
@HiltViewModel
class LocationSetupViewModel @Inject constructor(
    private val location: LocationRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving.asStateFlow()

    private val resultChannel = Channel<LocationSetupResult>(Channel.BUFFERED)
    val results = resultChannel.receiveAsFlow()

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            resultChannel.trySend(LocationSetupResult.Denied)
            return
        }
        viewModelScope.launch {
            _resolving.value = true
            val city = location.currentLocation()?.let { location.cityAt(it) }
            _resolving.value = false
            if (city != null) {
                preferences.setCity(city)
                preferences.setOnboardingDone()
                resultChannel.send(LocationSetupResult.Home)
            } else {
                // Hay permiso pero no se pudo saber la ciudad: que la elija el usuario.
                resultChannel.send(LocationSetupResult.ChooseCity)
            }
        }
    }
}
