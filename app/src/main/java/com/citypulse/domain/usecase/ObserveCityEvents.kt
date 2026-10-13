package com.citypulse.domain.usecase

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.City
import com.citypulse.domain.model.CityEvents
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Si el usuario está a más de esta distancia de la ciudad elegida, se usa el centro de la ciudad. */
private const val MAX_USER_DISTANCE_FROM_CITY_METERS = 50_000.0

/** Observa los eventos de la ciudad elegida, ya con la distancia a pie desde el usuario. */
class ObserveCityEvents @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val events: EventRepository,
    private val location: LocationRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<CityEvents?> =
        preferences.preferences
            .map { it.city }
            .distinctUntilChanged()
            .flatMapLatest { city ->
                if (city == null) {
                    flowOf(null)
                } else {
                    val (origin, isUser) = referencePoint(city)
                    events.observeEvents(city).map { list ->
                        CityEvents(city, origin, isUser, list.withDistanceFrom(origin))
                    }
                }
            }

    private suspend fun referencePoint(city: City) =
        location.currentLocation()
            ?.takeIf { distanceMeters(it, city.center) <= MAX_USER_DISTANCE_FROM_CITY_METERS }
            ?.let { it to true }
            ?: (city.center to false)
}
