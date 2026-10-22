package com.citypulse.data.ai

import android.util.Log
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.repository.AiQuotaExceededException
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
private const val PRICES_TIMEOUT_MS = 90_000L

/** Eventos por consulta de precios: una sola petición por ciudad cubre la semana. */
private const val PRICES_BATCH = 80

/**
 * Modelos de Gemini (sin facturación en la Gemini Developer API vía Firebase AI Logic), por
 * orden de preferencia. Cada modelo tiene su propia cuota diaria gratuita: primero los "Lite",
 * pensados para mucho volumen y más rápidos; si uno se agota o se satura, se pasa al siguiente.
 */
private val MODELS = listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.5-flash", "gemini-3.8-flash")

/** Si un modelo agota su cuota sin decir cuándo vuelve, se descarta durante este tiempo. */
private const val DEFAULT_QUOTA_PAUSE_MS = 60 * 60 * 1000L
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
/** Respuesta estructurada de [GeminiEventAssistant.estimatePrices]; coincide con [pricesSchema]. */
@Serializable
internal data class PriceEstimateDto(val id: String = "", val price: Double? = null)

private val pricesSchema = Schema.array(
    Schema.obj(mapOf("id" to Schema.string(), "price" to Schema.double())),
)

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
    private val priceModels by lazy {
        MODELS.map { name ->
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = pricesSchema
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

    /** Modelo → instante (ms) hasta el que no se usa porque agotó su cuota. */
    private val exhaustedUntil = ConcurrentHashMap<String, Long>()

    /**
     * Pide la respuesta a los modelos por orden, saltando los que tienen la cuota agotada. Si uno
     * se cuelga o está saturado, prueba el siguiente tras una breve espera.
     */
    private suspend fun generate(models: List<GenerativeModel>, prompt: String, timeoutMs: Long): String =
        withTimeout(timeoutMs) {
            var lastError: Throwable? = null
            for ((index, model) in models.withIndex()) {
                val name = MODELS[index]
                if ((exhaustedUntil[name] ?: 0L) > System.currentTimeMillis()) continue
                try {
                    // Cada modelo tiene su propio límite: si uno se queda colgado se pasa al siguiente.
                    val text = withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) { model.generateContent(prompt).text?.trim() }
                    if (!text.isNullOrBlank()) return@withTimeout text
                    lastError = IllegalStateException("$name: sin respuesta en ${ATTEMPT_TIMEOUT_MS / 1000} s")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    if (e.isQuotaError()) {
                        exhaustedUntil[name] = System.currentTimeMillis() + (e.retryDelayMs() ?: DEFAULT_QUOTA_PAUSE_MS)
                    } else {
                        delay(RETRY_DELAY_MS)
                    }
                }
                Log.w(TAG, "$name no respondió: ${lastError?.message?.lineSequence()?.firstOrNull()}")
            }
            // Si todos los modelos están sin cuota, se avisa de ello en concreto.
            if (MODELS.all { (exhaustedUntil[it] ?: 0L) > System.currentTimeMillis() }) throw AiQuotaExceededException()
            throw lastError ?: AiQuotaExceededException()
        }

    private fun Throwable.isQuotaError(): Boolean =
        message.orEmpty().let { "quota" in it.lowercase() || "exceeded" in it.lowercase() || "429" in it }

    /** Lee "Please retry in 1h11m49s" del mensaje de error. */
    private fun Throwable.retryDelayMs(): Long? {
        val match = Regex("""retry in (?:(\d+)h)?(?:(\d+)m)?(?:([\d.]+)s)?""").find(message.orEmpty()) ?: return null
        val (h, m, sec) = match.destructured
        val total = (h.toLongOrNull() ?: 0) * 3_600_000 + (m.toLongOrNull() ?: 0) * 60_000 + ((sec.toDoubleOrNull() ?: 0.0) * 1000).toLong()
        return total.takeIf { it > 0 }
    }

    // Cachés: repetir una búsqueda o volver a ver un resumen o una sorpresa no gasta cuota.
    private val summaries = ConcurrentHashMap<String, String>()
    private val intents = ConcurrentHashMap<String, SearchIntent>()
    private val explanations = ConcurrentHashMap<String, String>()

    override suspend fun interpret(query: String, cityName: String, currency: String, now: LocalDateTime) = runCatching {
        val cacheKey = "$cityName|${now.toLocalDate()}|${now.hour / 6}|${query.trim().lowercase()}"
        intents[cacheKey]?.let { return@runCatching it }
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
        json.decodeFromString<SearchIntentDto>(text).toDomain().also { intents[cacheKey] = it }
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
        explanations[event.event.id]?.let { return@runCatching it }
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
        generate(textModels, prompt, EXPLAIN_TIMEOUT_MS).trim('"').also { explanations[event.event.id] = it }
    }.onFailure { Log.w(TAG, "explainSurprise", it) }

    override suspend fun estimatePrices(events: List<Event>, cityName: String, currency: String) = runCatching {
        if (events.isEmpty()) return@runCatching emptyMap()
        val batch = events.take(PRICES_BATCH)
        val lines = batch.joinToString("\n") { e ->
            "${e.id} | ${e.title.take(80)} | ${e.venue.take(50)} | ${e.category.label}"
        }
        val prompt = """
            Eres un experto en ocio de $cityName. Estos eventos no publican su precio.
            Para cada uno, estima el precio típico de la entrada más barata en $currency, según el artista
            o espectáculo, el recinto y el tipo de evento. Usa cifras redondas y realistas para $cityName.
            Si de verdad no puedes estimarlo, omite ese evento. Responde con el mismo id de cada línea.
            Formato de cada línea: id | título | recinto | categoría
            $lines
        """.trimIndent()
        val text = generate(priceModels, prompt, PRICES_TIMEOUT_MS)
        val ids = batch.map { it.id }.toSet()
        json.decodeFromString<List<PriceEstimateDto>>(text)
            .filter { it.id in ids && (it.price ?: -1.0) > 0.0 }
            .associate { it.id to it.price!! }
    }.onFailure { Log.w(TAG, "estimatePrices", it) }
}
