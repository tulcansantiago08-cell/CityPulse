package com.citypulse.domain.repository

import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.SearchIntent
import java.time.LocalDateTime

/** Se agotó la cuota gratuita de IA de todos los modelos; se renueva cada día. */
class AiQuotaExceededException : Exception("Cuota de IA agotada")

/** Funciones de IA de la app. Si la IA no está disponible, devuelven un fallo y la app sigue. */
interface EventAssistant {
    /** Convierte una petición en lenguaje natural en filtros. */
    suspend fun interpret(query: String, cityName: String, currency: String, now: LocalDateTime): Result<SearchIntent>

    /** Resumen en español de 2–3 frases (traduce si el original está en otro idioma). */
    suspend fun summarize(event: Event): Result<String>

    /** Una frase que explica por qué se sugiere este evento en la Sorpresa. */
    suspend fun explainSurprise(event: NearbyEvent, now: LocalDateTime): Result<String>
}
