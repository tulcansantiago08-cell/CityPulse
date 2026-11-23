package com.citypulse.domain.model

import com.citypulse.domain.i18n.tr

/**
 * Categorías de evento que muestra la app (las mismas del prototipo). [label] es el nombre en español
 * que usan la búsqueda y la IA; [displayLabel] es el que se muestra, en el idioma de la app.
 */
enum class Category(val label: String, private val english: String) {
    Music("Música", "Music"),
    Markets("Mercados", "Markets"),
    Fairs("Ferias", "Fairs"),
    Culture("Cultura", "Culture"),
    Food("Comida", "Food"),
    Art("Arte", "Art"),
    ;

    val displayLabel: String get() = tr(label, english)
}
