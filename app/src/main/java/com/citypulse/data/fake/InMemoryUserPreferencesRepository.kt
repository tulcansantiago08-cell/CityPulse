package com.citypulse.data.fake

import com.citypulse.domain.model.City
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Preferencias en memoria (se pierden al cerrar la app). */
@Singleton
class InMemoryUserPreferencesRepository @Inject constructor() : UserPreferencesRepository {

    private val state = MutableStateFlow(UserPreferences(city = SampleData.Madrid))

    override val preferences: Flow<UserPreferences> = state

    override suspend fun setCity(city: City) = state.update { it.copy(city = city) }
    override suspend fun setFreeOnly(freeOnly: Boolean) = state.update { it.copy(freeOnly = freeOnly) }
    override suspend fun setRadiusMinutes(minutes: Int) = state.update { it.copy(radiusMinutes = minutes) }
    override suspend fun setOnboardingDone() = state.update { it.copy(onboardingDone = true) }
}
