package com.citypulse.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Fuente de la hora actual; se inyecta para poder fijarla en los tests.
 *
 * Trabaja con instantes: la hora "local" depende de la ciudad que se consulta, no del teléfono
 * (desde Bogotá, "ahora" en Madrid son 7 horas más).
 */
interface Clock {
    fun instant(): Instant

    /** Emite el instante actual cada [periodMillis] (por defecto, cada 30 s como el prototipo). */
    fun ticks(periodMillis: Long = 30_000): Flow<Instant> = flow {
        while (true) {
            emit(instant())
            delay(periodMillis)
        }
    }
}

/** Hora local en [zone] para este instante. */
fun Instant.localIn(zone: ZoneId): LocalDateTime = LocalDateTime.ofInstant(this, zone)

/** Hora local actual en [zone] (por defecto, la del teléfono). */
fun Clock.now(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime = instant().localIn(zone)

class SystemClock : Clock {
    override fun instant(): Instant = Instant.now()
}
