package com.citypulse

import android.app.Application
import android.content.Context
import com.citypulse.ui.language.LanguageManager
import com.citypulse.data.reminders.EventReminderScheduler
import com.citypulse.data.sync.SavedEventsCloudSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CityPulseApp : Application() {

    @Inject lateinit var savedSync: SavedEventsCloudSync
    @Inject lateinit var reminders: EventReminderScheduler

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        // Español por defecto; inglés si la persona lo eligió.
        LanguageManager.applyAtStartup(this)
        // App Check: Firebase solo acepta peticiones de IA que vengan de esta app.
        installAppCheck(this)
        // Con sesión iniciada, los guardados se copian en la cuenta.
        savedSync.start()
        // Aviso una hora antes de cada evento guardado.
        reminders.start()
    }
}
