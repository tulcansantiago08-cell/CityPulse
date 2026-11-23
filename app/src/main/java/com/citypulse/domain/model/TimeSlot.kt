package com.citypulse.domain.model

import com.citypulse.domain.i18n.tr

/** Franja del día con la que se filtran las sugerencias del modo "Estoy aquí ahora". */
enum class TimeSlot(val label: String, private val english: String) {
    Morning("Mañana", "Morning"),
    Afternoon("Tarde", "Afternoon"),
    Night("Noche", "Night"),
    ;

    /** El nombre en el idioma de la app. */
    val displayLabel: String get() = tr(label, english)
}
