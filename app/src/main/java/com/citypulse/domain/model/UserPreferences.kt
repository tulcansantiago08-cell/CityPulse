package com.citypulse.domain.model

/** Preferencias del usuario que se conservan entre sesiones. */
data class UserPreferences(
    val city: City? = null,
    val freeOnly: Boolean = false,
    val priceRange: PriceRange = PriceRange.Any,
    val radiusMinutes: Int = DEFAULT_RADIUS_MINUTES,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_RADIUS_MINUTES = 15
        const val MIN_RADIUS_MINUTES = 5
        const val MAX_RADIUS_MINUTES = 30
    }
}
