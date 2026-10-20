package com.citypulse

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CityPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // App Check: Firebase solo acepta peticiones de IA que vengan de esta app.
        installAppCheck()
    }
}
