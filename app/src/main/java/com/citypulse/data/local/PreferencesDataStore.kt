package com.citypulse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Único DataStore de preferencias de la app. */
val Context.cityPulseDataStore: DataStore<Preferences> by preferencesDataStore(name = "citypulse_prefs")
