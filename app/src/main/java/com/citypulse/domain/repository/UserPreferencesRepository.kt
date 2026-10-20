package com.citypulse.domain.repository

import com.citypulse.domain.model.City
import com.citypulse.domain.model.PriceRange
import com.citypulse.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setCity(city: City)
    suspend fun setFreeOnly(freeOnly: Boolean)
    suspend fun setPriceRange(range: PriceRange)
    suspend fun setRadiusMinutes(minutes: Int)
    suspend fun setOnboardingDone()
}
