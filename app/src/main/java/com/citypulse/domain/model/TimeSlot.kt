package com.citypulse.domain.model

/** Franja del día con la que se filtran las sugerencias del modo "Estoy aquí ahora". */
enum class TimeSlot(val label: String) {
    Morning("Mañana"),
    Afternoon("Tarde"),
    Night("Noche"),
}
