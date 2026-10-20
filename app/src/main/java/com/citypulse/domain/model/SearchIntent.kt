package com.citypulse.domain.model

/**
 * Lo que el usuario pide en lenguaje natural, ya convertido en filtros por la IA
 * ("algo gratis de música esta noche" → música + gratis + noche + hoy).
 */
data class SearchIntent(
    val categories: Set<Category> = emptySet(),
    val freeOnly: Boolean = false,
    /** Precio máximo en la moneda de la ciudad. */
    val maxPrice: Double? = null,
    val timeSlot: TimeSlot? = null,
    /** Solo eventos de hoy ("esta noche", "ahora"). */
    val todayOnly: Boolean = false,
    /** Solo lo que ya está en curso ("ahora mismo"). */
    val liveNow: Boolean = false,
    val maxWalkMinutes: Int? = null,
    /** Nombres concretos que deben aparecer (artista, lugar, plato…). */
    val keywords: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = categories.isEmpty() && !freeOnly && maxPrice == null && timeSlot == null &&
            !todayOnly && !liveNow && maxWalkMinutes == null && keywords.isEmpty()
}
