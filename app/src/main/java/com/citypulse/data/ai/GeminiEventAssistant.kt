package com.citypulse.data.ai

import android.util.Log
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.repository.EventAssistant
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "CityPulseAI"

/**
 * Espera máxima por petición. La capa gratuita a veces tarda más de un minuto; como la app no
 * se bloquea mientras tanto (resultados básicos al instante, frase y resumen cuando llegan),
 * se espera hasta 90 s y luego se sigue sin IA en vez de dejar una carga infinita.
 */
private const val INTERPRET_TIMEOUT_MS = 90_000L
private const val SUMMARY_TIMEOUT_MS = 90_000L
private const val EXPLAIN_TIMEOUT_MS = 90_000L

/**
 * Modelos de Gemini (sin facturación en la Gemini Developer API vía Firebase AI Logic), por
 * orden de preferencia. Si el primero está saturado se prueba el siguiente.
 */
private val MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite")
private const val RETRY_DELAY_MS = 1_500L
private const val ATTEMPT_TIMEOUT_MS = 25_000L

/** Respuesta estructurada de [GeminiEventAssistant.interpret]; coincide con [intentSchema]. */
@Serializable
internal data class SearchIntentDto(
    val categories: List<String> = emptyList(),
    val freeOnly: Boolean = false,
    val maxPrice: Double? = null,
    val timeSlot: String? = null,
    val todayOnly: Boolean = false,
    val liveNow: Boolean = false,
    val maxWalkMinutes: Int? = null,
    val keywords: List<String> = emptyList(),
) {
    fun toDomain() = SearchIntent(
        categories = categories.mapNotNull { name -> Category.entries.firstOrNull { it.name == name } }.toSet(),
        freeOnly = freeOnly,
        maxPrice = maxPrice?.takeIf { it > 0 },
        timeSlot = TimeSlot.entries.firstOrNull { it.name == timeSlot },
        todayOnly = todayOnly,
        liveNow = liveNow,
        maxWalkMinutes = maxWalkMinutes?.takeIf { it > 0 },
        keywords = keywords.filter { it.isNotBlank() }.take(3),
    )
}

private val intentSchema = Schema.obj(
    mapOf(
        "categories" to Schema.array(Schema.enumeration(Category.entries.map { it.name })),
        "freeOnly" to Schema.boolean(),
        "maxPrice" to Schema.double(),
        "timeSlot" to Schema.enumeration(TimeSlot.entries.map { it.name }),
        "todayOnly" to Schema.boolean(),
        "liveNow" to Schema.boolean(),
        "maxWalkMinutes" to Schema.integer(),
        "keywords" to Schema.array(Schema.string()),
    ),
    optionalProperties = listOf("maxPrice", "timeSlot", "maxWalkMinutes"),
)

@Singleton
class GeminiEventAssistant @Inject constructor() : EventAssistant {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Se crean al primer uso: si AI Logic no está activado, falla esa llamada y no el arranque.
    private val jsonModels by lazy {
        MODELS.map { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = intentSchema
                    temperature = 0f
                },
            )
        }
    }
    private val textModels by lazy {
        MODELS.map { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig { temperature = 0.4f },
            )
        }
    }

    /**
     * Pide la respuesta al modelo principal y, si está saturado o falla, a los alternativos,
     * con una breve espera entre intentos. Así un pico de demanda no deja la función sin IA.
     */
    private suspend fun generate(models: List<GenerativeModel>, prompt: String, timeoutMs: Long): String =
        withTimeout(timeoutMs) {
            var lastError: Throwable? = null
            for ((index, model) in models.withIndex()) {
                try {
                    // Cada modelo tiene su propio límite: si uno se queda colgado se pasa al siguiente.
                    val text = withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) { model.generateContent(prompt).text?.trim() }
                    if (!text.isNullOrBlank()) return@withTimeout text
                    lastError = IllegalStateException("${MODELS[index]}: sin respuesta en ${ATTEMPT_TIMEOUT_MS / 1000} s")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    delay(RETRY_DELAY_MS)
                }
                Log.w(TAG, "${MODELS[index]} no respondió: ${lastError?.message}")
            }
            throw lastError ?: IllegalStateException("Sin modelos")
        }

    /** Los resúmenes no cambian: se guardan para no gastar la cuota gratuita dos veces. */
    private val summaries = ConcurrentHashMap<String, String>()

    override suspend fun interpret(query: String, cityName: String, currency: String, now: LocalDateTime) = runCatching {
        val prompt = """
            Eres el buscador de CityPulse, una app de eventos para viajeros en $cityName.
            Ahora es ${now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es")))}.
            Convierte la petición del usuario en filtros. Reglas:
            - categories: solo las que la petición pida (Music, Markets, Fairs, Culture, Food, Art). Vacío si no concreta.
            - freeOnly: true si pide gratis/sin pagar.
            - maxPrice: tope de precio en $currency si lo menciona ("barato" en EUR ≈ 10; en COP ≈ 20000).
            - timeSlot: Morning (6-14 h), Afternoon (14-20 h) o Night (20-6 h) si menciona mañana/tarde/noche.
            - todayOnly: true si dice hoy, esta noche, esta tarde o ahora.
            - liveNow: true solo si pide algo que esté pasando ya mismo.
            - maxWalkMinutes: si pide algo cerca (≈15) o da minutos/distancia a pie.
            - keywords: solo nombres concretos (artista, lugar, plato, estilo como "jazz" o "flamenco"); nunca adjetivos como "tranquilo" o "bonito".
            Petición: "$query"
        """.trimIndent()
        val text = generate(jsonModels, prompt, INTERPRET_TIMEOUT_MS)
        json.decodeFromString<SearchIntentDto>(text).toDomain()
    }.onFailure { Log.w(TAG, "interpret", it) }

    override suspend fun summarize(event: Event) = runCatching {
        summaries[event.id] ?: run {
            val prompt = """
                Resume en español, en 2 o 3 frases claras y sin inventar datos, este evento para un viajero.
                Si el texto está en otro idioma, tradúcelo. No repitas el horario ni el precio.
                Título: ${event.title}
                Lugar: ${event.venue}, ${event.area}
                Categoría: ${event.category.label}
                Descripción: ${event.description}
            """.trimIndent()
            generate(textModels, prompt, SUMMARY_TIMEOUT_MS).also { summaries[event.id] = it }
        }
    }.onFailure { Log.w(TAG, "summarize", it) }

    override suspend fun explainSurprise(event: NearbyEvent, now: LocalDateTime) = runCatching {
        val e = event.event
        val timing = when {
            e.isLive(now) -> "ya está en curso"
            else -> "empieza a las ${e.start.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        val prompt = """
            Escribe UNA frase corta (máximo 20 palabras), cercana y en español, que anime a ir a este plan.
            Usa solo estos datos, sin inventar nada: "${e.title}" (${e.category.label}) en ${e.venue};
            está a ${event.walkMinutes} minutos a pie; $timing; ${if (e.isFree) "es gratis" else "precio: ${e.price ?: "en la web del evento"}"}.
            Ahora son las ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}. Sin comillas ni emojis.
        """.trimIndent()
        generate(textModels, prompt, EXPLAIN_TIMEOUT_MS).trim('"')
    }.onFailure { Log.w(TAG, "explainSurprise", it) }
}
