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
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cities: CityRepository,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val name = prefs[Keys.cityName]
        val lat = prefs[Keys.cityLat]
        val lng = prefs[Keys.cityLng]
        UserPreferences(
            city = if (name != null && lat != null && lng != null) {
                City(name, prefs[Keys.cityCountry].orEmpty(), GeoPoint(lat, lng), zoneFor(name, prefs[Keys.cityZone]))
            } else {
                null
            },
            freeOnly = prefs[Keys.freeOnly] ?: false,
            budgetStep = prefs[Keys.budgetStep],
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
            it[Keys.cityZone] = city.zone.id
        }
    }

    override suspend fun setFreeOnly(freeOnly: Boolean) {
        dataStore.edit { it[Keys.freeOnly] = freeOnly }
    }

    override suspend fun setBudgetStep(step: Int?) {
        dataStore.edit {
            if (step == null) it.remove(Keys.budgetStep) else it[Keys.budgetStep] = step
            it.remove(Keys.legacyPriceRange)
        }
    }

    override suspend fun setRadiusMinutes(minutes: Int) {
        dataStore.edit { it[Keys.radius] = minutes }
    }

    override suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.onboardingDone] = true }
    }

    /** Zona guardada; si no hay (versiones anteriores), la de la ciudad conocida o la del teléfono. */
    private fun zoneFor(name: String, stored: String?): ZoneId =
        stored?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: cities.popularCities().firstOrNull { it.name == name }?.zone
            ?: ZoneId.systemDefault()

    private object Keys {
        val cityName = stringPreferencesKey("city_name")
        val cityCountry = stringPreferencesKey("city_country")
        val cityLat = doublePreferencesKey("city_lat")
        val cityLng = doublePreferencesKey("city_lng")
        val cityZone = stringPreferencesKey("city_zone")
        val freeOnly = booleanPreferencesKey("free_only")
        val budgetStep = intPreferencesKey("budget_step")

        /** Filtro de rangos de versiones anteriores; se borra al elegir presupuesto. */
        val legacyPriceRange = stringPreferencesKey("price_range")
        val radius = intPreferencesKey("radius_minutes")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }
}
