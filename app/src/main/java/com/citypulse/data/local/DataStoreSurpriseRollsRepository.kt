package com.citypulse.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.citypulse.domain.model.SurpriseRolls
import com.citypulse.domain.repository.SurpriseRollsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSurpriseRollsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SurpriseRollsRepository {

    override fun observe(today: LocalDate): Flow<SurpriseRolls> = dataStore.data.map { prefs ->
        val date = prefs[Keys.date]?.let(LocalDate::parse) ?: today
        SurpriseRolls(
            date = date,
            used = prefs[Keys.used] ?: 0,
            currentEventId = prefs[Keys.current],
            seenEventIds = prefs[Keys.seen].orEmpty(),
        ).on(today)
    }

    override suspend fun save(rolls: SurpriseRolls) {
        dataStore.edit { prefs ->
            prefs[Keys.date] = rolls.date.toString()
            prefs[Keys.used] = rolls.used
            prefs[Keys.seen] = rolls.seenEventIds
            val current = rolls.currentEventId
            if (current != null) prefs[Keys.current] = current else prefs.remove(Keys.current)
        }
    }

    private object Keys {
        val date = stringPreferencesKey("surprise_date")
        val used = intPreferencesKey("surprise_used")
        val current = stringPreferencesKey("surprise_current")
        val seen = stringSetPreferencesKey("surprise_seen")
    }
}
