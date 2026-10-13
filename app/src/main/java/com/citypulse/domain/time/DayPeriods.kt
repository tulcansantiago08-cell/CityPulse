package com.citypulse.domain.time

import com.citypulse.domain.model.TimeSlot
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Franja horaria según la hora: mañana de 6 a 14 h, tarde de 14 a 20 h y noche de 20 a 6 h
 * (la misma regla que el prototipo).
 */
fun timeSlotFor(time: LocalTime): TimeSlot = when (time.hour) {
    in 6..13 -> TimeSlot.Morning
    in 14..19 -> TimeSlot.Afternoon
    else -> TimeSlot.Night
}

/** Intervalo [inicio, fin) que cubre una franja para el "día" en curso. */
data class TimeWindow(val start: LocalDateTime, val end: LocalDateTime) {
    fun overlaps(from: LocalDateTime, to: LocalDateTime): Boolean = from.isBefore(end) && to.isAfter(start)
}

/**
 * Ventana de la franja [slot] para el día que vive el usuario en [now]. Antes de las 6 h
 * se considera que todavía es la noche del día anterior.
 */
fun TimeSlot.windowFor(now: LocalDateTime): TimeWindow {
    val day = if (now.hour < 6) now.toLocalDate().minusDays(1) else now.toLocalDate()
    return when (this) {
        TimeSlot.Morning -> TimeWindow(day.atTime(6, 0), day.atTime(14, 0))
        TimeSlot.Afternoon -> TimeWindow(day.atTime(14, 0), day.atTime(20, 0))
        TimeSlot.Night -> TimeWindow(day.atTime(20, 0), day.plusDays(1).atTime(6, 0))
    }
}

/** Saludo de la pantalla de inicio: días de 6 a 13 h, tardes de 13 a 20 h y noches el resto. */
enum class Greeting { Morning, Afternoon, Evening }

fun greetingFor(time: LocalTime): Greeting = when (time.hour) {
    in 6..12 -> Greeting.Morning
    in 13..19 -> Greeting.Afternoon
    else -> Greeting.Evening
}
