package com.citypulse.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.citypulse.data.local.db.SavedEventDao
import com.citypulse.data.local.db.SavedEventEntity
import com.citypulse.domain.time.Clock
import com.citypulse.domain.usecase.reminderInstant
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Programa un aviso una hora antes de cada evento guardado.
 *
 * Usa alarmas inexactas (no hace falta precisión al minuto, y no pide permisos especiales).
 * No cancela las alarmas de los eventos que se quitan de guardados: al sonar, el receptor
 * comprueba que el evento siga guardado y, si no, no avisa.
 */
@Singleton
class EventReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SavedEventDao,
    private val clock: Clock,
) {
    private val alarms: AlarmManager? = context.getSystemService(AlarmManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Reprograma los avisos cada vez que cambian los guardados (también los que llegan de la nube). */
    fun start() {
        scope.launch { dao.observeAll().collect { scheduleAll(it) } }
    }

    /** Tras reiniciar el teléfono o actualizar la app, el sistema borra las alarmas: se vuelven a poner. */
    suspend fun rescheduleAll() = scheduleAll(dao.getAll())

    private fun scheduleAll(events: List<SavedEventEntity>) {
        val now = clock.instant()
        events.forEach { schedule(it, now) }
    }

    private fun schedule(event: SavedEventEntity, now: Instant) {
        val alarms = alarms ?: return
        val start = runCatching { LocalDateTime.parse(event.start) }.getOrNull() ?: return
        val at = reminderInstant(start, event.zoneId(), now) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), reminderIntent(context, event.id))
    }

    companion object {
        const val ACTION_REMIND = "com.citypulse.action.REMIND_EVENT"
        const val EXTRA_EVENT_ID = "eventId"

        /** Un PendingIntent por evento: volver a programarlo sustituye la alarma anterior. */
        fun reminderIntent(context: Context, eventId: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            Intent(context, EventReminderReceiver::class.java)
                .setAction(ACTION_REMIND)
                .putExtra(EXTRA_EVENT_ID, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Zona de la ciudad del evento; los guardados antes de la v6 no la tienen y usan la del teléfono. */
internal fun SavedEventEntity.zoneId(): ZoneId =
    zone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
