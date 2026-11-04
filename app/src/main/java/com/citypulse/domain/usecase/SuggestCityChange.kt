package com.citypulse.domain.usecase

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.City
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

/**
 * Ciudad donde está el usuario, cuando no es la elegida: quien viaja puede querer cambiar. Solo se
 * sugiere; la app no cambia sola, para no sacar de la ciudad a quien la está explorando desde lejos.
 */
class SuggestCityChange @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val location: LocationRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<City?> =
        combine(preferences.preferences.map { it.city }.distinctUntilChanged(), location.observeLocation()) { city, user -> city to user }
            .distinctUntilChanged()
            .mapLatest { (city, user) ->
                if (city == null || user == null || distanceMeters(user, city.center) <= MAX_USER_DISTANCE_FROM_CITY_METERS) {
                    null
                } else {
                    runCatching { location.cityAt(user) }.getOrNull()?.takeIf { it.key != city.key }
                }
            }
            .distinctUntilChanged()
}
