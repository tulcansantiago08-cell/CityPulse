package com.citypulse.domain.usecase

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.text.normalizedForSearch

// Español e inglés: la búsqueda sin IA entiende las dos.
private val CategoryWords = mapOf(
    Category.Music to listOf("musica", "concierto", "jazz", "flamenco", "dj", "banda", "rock", "salsa", "music", "concert", "concerts", "band", "gig", "live"),
    Category.Markets to listOf("mercado", "mercadillo", "pulgas", "rastro", "market", "markets", "flea"),
    Category.Fairs to listOf("feria", "festival", "fair", "fairs", "festivals"),
    Category.Culture to listOf("cultura", "cultural", "teatro", "cine", "poesia", "danza", "yoga", "culture", "theater", "theatre", "cinema", "movie", "movies", "poetry", "dance"),
    Category.Food to listOf("comida", "comer", "tapas", "tapeo", "gastronomia", "cenar", "almorzar", "food", "eat", "dinner", "lunch", "brunch"),
    Category.Art to listOf("arte", "museo", "exposicion", "pintura", "galeria", "art", "museum", "exhibition", "painting", "gallery"),
)

private val StopWords = setOf(
    "algo", "para", "esta", "este", "esto", "unos", "unas", "quiero", "busco", "plan", "planes",
    "donde", "puedo", "hacer", "evento", "eventos", "alguna", "alguno", "cosa", "cosas", "con", "que",
    "something", "want", "looking", "where", "with", "that", "this", "some", "event", "events", "plans", "things", "thing",
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
        has("noche", "esta noche", "tonight", "night", "evening") -> TimeSlot.Night
        has("tarde", "esta tarde", "por la tarde", "afternoon") -> TimeSlot.Afternoon
        has("por la manana", "en la manana", "esta manana", "morning") -> TimeSlot.Morning
        else -> null
    }
    val known = CategoryWords.values.flatten().toSet() + setOf(
        "gratis", "gratuito", "gratuita", "hoy", "noche", "tarde", "manana", "ahora", "mismo", "ya", "cerca", "barato", "barata", "por", "la", "en",
        "free", "today", "tonight", "night", "evening", "afternoon", "morning", "now", "right", "near", "nearby", "cheap", "the", "for",
    )
    val leftovers = words.filter { it.length >= 4 && it !in known && it !in StopWords }
    return SearchIntent(
        categories = categories,
        freeOnly = has("gratis", "gratuito", "gratuita", "sin pagar", "free"),
        timeSlot = slot,
        todayOnly = has("hoy", "esta noche", "esta tarde", "esta manana", "ahora", "today", "tonight", "now"),
        liveNow = has("ahora mismo", "ya mismo", "right now"),
        maxWalkMinutes = if (has("cerca", "cerquita", "caminando", "near", "nearby", "walking")) 15 else null,
        // Palabras sueltas solo si no se reconoció nada más (p. ej. el nombre de un lugar).
        keywords = if (categories.isEmpty() && slot == null && !has("gratis", "free")) leftovers.take(3) else emptyList(),
    )
}
