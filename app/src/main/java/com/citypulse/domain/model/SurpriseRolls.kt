package com.citypulse.domain.model

import java.time.LocalDate

/**
 * Tiradas del modo Sorpresa en un día. Cada evento sugerido cuenta como una tirada,
 * incluida la primera, y el contador vuelve a cero al cambiar de día.
 */
data class SurpriseRolls(
    val date: LocalDate,
    val used: Int = 0,
    val currentEventId: String? = null,
    val seenEventIds: Set<String> = emptySet(),
) {
    fun remaining(max: Int): Int = (max - used).coerceAtLeast(0)

    fun canRoll(max: Int): Boolean = used < max

    /** Devuelve el estado de hoy, empezando de cero si el guardado es de otro día. */
    fun on(today: LocalDate): SurpriseRolls = if (date == today) this else SurpriseRolls(today)

    fun afterPicking(eventId: String): SurpriseRolls =
        copy(used = used + 1, currentEventId = eventId, seenEventIds = seenEventIds + eventId)

    companion object {
        const val DEFAULT_MAX_PER_DAY = 5
    }
}
