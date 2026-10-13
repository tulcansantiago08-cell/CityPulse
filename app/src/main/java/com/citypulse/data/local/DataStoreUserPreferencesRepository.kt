package com.citypulse.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val name = prefs[Keys.cityName]
        val lat = prefs[Keys.cityLat]
        val lng = prefs[Keys.cityLng]
        UserPreferences(
            city = if (name != null && lat != null && lng != null) {
                City(name, prefs[Keys.cityCountry].orEmpty(), GeoPoint(lat, lng))
            } else {
                null
            },
            freeOnly = prefs[Keys.freeOnly] ?: false,
            radiusMinutes = prefs[Keys.radius] ?: UserPreferences.DEFAULT_RADIUS_MINUTES,
            onboardingDone = prefs[Keys.onboardingDone] ?: false,
        )
    }

    override suspend fun setCity(city: City) {
        dataStore.edit {
            it[Keys.cityName] = city.name
            it[Keys.cityCountry] = city.country
            it[Keys.cityLat] = city.center.latitude
            it[Keys.cityLng] = city.center.longitude
        }
    }

    override suspend fun setFreeOnly(freeOnly: Boolean) {
        dataStore.edit { it[Keys.freeOnly] = freeOnly }
    }

    override suspend fun setRadiusMinutes(minutes: Int) {
        dataStore.edit { it[Keys.radius] = minutes }
    }

    override suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.onboardingDone] = true }
    }

    private object Keys {
        val cityName = stringPreferencesKey("city_name")
        val cityCountry = stringPreferencesKey("city_country")
        val cityLat = doublePreferencesKey("city_lat")
        val cityLng = doublePreferencesKey("city_lng")
        val freeOnly = booleanPreferencesKey("free_only")
        val radius = intPreferencesKey("radius_minutes")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }
}
