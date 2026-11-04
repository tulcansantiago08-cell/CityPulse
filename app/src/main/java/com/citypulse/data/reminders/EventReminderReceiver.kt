package com.citypulse.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.citypulse.data.local.db.SavedEventDao
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.reminderInstant
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recibe la alarma de un evento guardado y muestra el aviso; también vuelve a programar
 * los avisos cuando el teléfono se reinicia o la app se actualiza.
 */
class EventReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun savedEventDao(): SavedEventDao
        fun scheduler(): EventReminderScheduler
        fun notifier(): EventReminderNotifier
        fun clock(): Clock
    }

    override fun onReceive(context: Context, intent: Intent) {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    EventReminderScheduler.ACTION_REMIND ->
                        intent.getStringExtra(EventReminderScheduler.EXTRA_EVENT_ID)?.let { remind(deps, it) }
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                        deps.scheduler().rescheduleAll()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun remind(deps: Dependencies, eventId: String) {
        // Si se quitó de guardados después de programar la alarma, no se avisa.
        val event = deps.savedEventDao().get(eventId) ?: return
        val start = runCatching { LocalDateTime.parse(event.start) }.getOrNull() ?: return
        // Tampoco si ya empezó (la alarma pudo retrasarse con el teléfono en reposo).
        if (reminderInstant(start, event.zoneId(), deps.clock().instant(), leadMinutes = 0) == null) return
        deps.notifier().notify(event, start)
    }
}
