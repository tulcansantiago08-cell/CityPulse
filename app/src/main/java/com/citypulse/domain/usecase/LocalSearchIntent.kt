package com.citypulse.domain.usecase

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.text.normalizedForSearch

private val CategoryWords = mapOf(
    Category.Music to listOf("musica", "concierto", "jazz", "flamenco", "dj", "banda", "rock", "salsa"),
    Category.Markets to listOf("mercado", "mercadillo", "pulgas", "rastro"),
    Category.Fairs to listOf("feria", "festival"),
    Category.Culture to listOf("cultura", "cultural", "teatro", "cine", "poesia", "danza", "yoga"),
    Category.Food to listOf("comida", "comer", "tapas", "tapeo", "gastronomia", "cenar", "almorzar"),
    Category.Art to listOf("arte", "museo", "exposicion", "pintura", "galeria"),
)

private val StopWords = setOf(
    "algo", "para", "esta", "este", "esto", "unos", "unas", "quiero", "busco", "plan", "planes",
    "donde", "puedo", "hacer", "evento", "eventos", "alguna", "alguno", "cosa", "cosas", "con", "que",
)

/**
 * Interpretación básica, sin IA, de las expresiones más comunes ("gratis", "esta noche",
 * "cerca", "música"…). Es el respaldo cuando Gemini no está disponible.
 */
fun localSearchIntent(query: String): SearchIntent {
    val text = " ${query.normalizedForSearch()} "
    val words = text.split(Regex("[^a-z0-9ñ]+")).filter { it.isNotBlank() }
    fun has(vararg options: String) = options.any { " $it " in text || text.contains(" $it") && it.contains(' ') }

    val categories = CategoryWords.filterValues { list -> list.any { it in words } }.keys
    val slot = when {
        has("noche", "esta noche") -> TimeSlot.Night
        has("tarde", "esta tarde", "por la tarde") -> TimeSlot.Afternoon
        has("por la manana", "en la manana", "esta manana") -> TimeSlot.Morning
        else -> null
    }
    val known = CategoryWords.values.flatten().toSet() + setOf(
        "gratis", "gratuito", "gratuita", "hoy", "noche", "tarde", "manana", "ahora", "mismo", "ya", "cerca", "barato", "barata", "por", "la", "en",
    )
    val leftovers = words.filter { it.length >= 4 && it !in known && it !in StopWords }
    return SearchIntent(
        categories = categories,
        freeOnly = has("gratis", "gratuito", "gratuita", "sin pagar"),
        timeSlot = slot,
        todayOnly = has("hoy", "esta noche", "esta tarde", "esta manana", "ahora"),
        liveNow = has("ahora mismo", "ya mismo"),
        maxWalkMinutes = if (has("cerca", "cerquita", "caminando")) 15 else null,
        // Palabras sueltas solo si no se reconoció nada más (p. ej. el nombre de un lugar).
        keywords = if (categories.isEmpty() && slot == null && !has("gratis")) leftovers.take(3) else emptyList(),
    )
}
