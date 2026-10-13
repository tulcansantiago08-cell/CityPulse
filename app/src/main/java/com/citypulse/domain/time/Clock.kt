package com.citypulse.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/** Fuente de la hora actual; se inyecta para poder fijarla en los tests. */
interface Clock {
    fun now(): LocalDateTime

    /** Emite la hora actual cada [periodMillis] (por defecto, cada 30 s como el prototipo). */
    fun ticks(periodMillis: Long = 30_000): Flow<LocalDateTime> = flow {
        while (true) {
            emit(now())
            delay(periodMillis)
        }
    }
}

class SystemClock : Clock {
    override fun now(): LocalDateTime = LocalDateTime.now()
}
