package com.citypulse.domain.usecase

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Con cuánta antelación se avisa de un evento guardado. */
const val REMINDER_LEAD_MINUTES = 60L

/**
 * Instante en que hay que avisar de un evento que empieza a la hora local [start] de [zone]
 * (la zona de la ciudad del evento, no la del teléfono), o `null` si ese momento ya pasó.
 */
fun reminderInstant(
    start: LocalDateTime,
    zone: ZoneId,
    now: Instant,
    leadMinutes: Long = REMINDER_LEAD_MINUTES,
): Instant? {
    val at = start.atZone(zone).toInstant().minus(Duration.ofMinutes(leadMinutes))
    return at.takeIf { it.isAfter(now) }
}
