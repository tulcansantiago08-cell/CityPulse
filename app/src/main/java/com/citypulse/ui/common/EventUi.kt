package com.citypulse.ui.common

import androidx.compose.ui.graphics.Color
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import java.net.URLEncoder
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Datos de un evento ya formateados para pintarlos en cualquier tarjeta. */
data class EventUi(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val venue: String,
    val area: String,
    val imageUrl: String?,
    val fallback: List<Color>,
    val live: Boolean,
    val free: Boolean,
    /** "Gratis", el precio, "Ver precio" (está en la web del evento) o "En taquilla". */
    val priceLabel: String,
    /** El precio no se conoce pero se puede consultar en la web del evento ([url]). */
    val priceOnline: Boolean,
    /**
     * Dónde conseguir entradas: la página de venta si existe; si no, una búsqueda web del evento.
     * `null` en los gratuitos (entrada libre).
     */
    val ticketsUrl: String?,
    val walkMinutes: Int,
    /** "Música · hasta 23:30" / "Música · empieza 22:00" */
    val overline: String,
    /** "Hoy · 21:00", "Mié 14 · 08:00" */
    val whenLabel: String,
    /** Fecha absoluta para compartir: "martes 20 oct · 21:00" (no "Hoy", que caduca). */
    val absoluteWhen: String,
    /** El evento ya terminó (p. ej. uno guardado de días anteriores). */
    val ended: Boolean,
    /** "21:00–23:30" */
    val timeRange: String,
    /** "0,5 km · 6 min a pie" */
    val distanceLabel: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val url: String?,
)

private val HourFormat = DateTimeFormatter.ofPattern("HH:mm")
private val SpanishDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val SpanishDaysLong = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val SpanishMonths = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

fun LocalDateTime.hourLabel(): String = format(HourFormat)

/** "Hoy", "Mañana" o "Mié 14". */
fun LocalDate.dayLabel(today: LocalDate): String = when (this) {
    today -> "Hoy"
    today.plusDays(1) -> "Mañana"
    else -> "${SpanishDays[dayOfWeek.value - 1]} $dayOfMonth"
}

/** Línea de fecha de la pantalla de inicio: "Martes 13 oct · 21:04". */
fun LocalDateTime.dateLine(): String =
    "${SpanishDaysLong[dayOfWeek.value - 1]} $dayOfMonth ${SpanishMonths[monthValue - 1]} · ${hourLabel()}"

/** Importe redondo con su moneda: "10 €", "20.000 COP". */
fun formatMoney(amount: Double, currency: String): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
        runCatching { this.currency = Currency.getInstance(currency) }
        maximumFractionDigits = 0
    }.format(amount)

fun distanceLabel(meters: Int, walkMinutes: Int): String {
    val km = String.format(Locale.forLanguageTag("es-ES"), "%.1f", meters / 1000.0)
    return "$km km · $walkMinutes min a pie"
}

fun NearbyEvent.toUi(now: LocalDateTime): EventUi {
    val live = event.isLive(now)
    val overline = if (live) "hasta ${event.end.hourLabel()}" else "empieza ${event.start.hourLabel()}"
    return EventUi(
        id = event.id,
        title = event.title,
        categoryLabel = event.category.label,
        venue = event.venue,
        area = event.area,
        imageUrl = event.imageUrl,
        fallback = event.category.fallbackColors(),
        live = live,
        free = event.isFree,
        priceLabel = priceLabel(event),
        priceOnline = !event.isFree && event.price == null && event.url != null,
        ticketsUrl = when {
            event.isFree -> null
            event.url != null -> event.url
            else -> ticketSearchUrl(event)
        },
        walkMinutes = walkMinutes,
        overline = "${event.category.label} · $overline",
        whenLabel = "${event.start.toLocalDate().dayLabel(now.toLocalDate())} · ${event.start.hourLabel()}",
        absoluteWhen = "${SpanishDaysLong[event.start.dayOfWeek.value - 1].lowercase()} ${event.start.dayOfMonth} ${SpanishMonths[event.start.monthValue - 1]} · ${event.start.hourLabel()}",
        ended = event.hasEnded(now),
        timeRange = "${event.start.hourLabel()}–${event.end.hourLabel()}",
        distanceLabel = distanceLabel(distanceMeters, walkMinutes),
        description = event.description,
        latitude = event.location.latitude,
        longitude = event.location.longitude,
        url = event.url,
    )
}

/** Búsqueda web del evento en su lugar, para eventos de pago sin página de venta propia. */
private fun ticketSearchUrl(event: Event): String {
    val query = URLEncoder.encode("${event.title} ${event.venue} ${event.area} entradas", Charsets.UTF_8.name())
    return "https://www.google.com/search?q=$query&hl=es"
}

private fun priceLabel(event: Event): String = when {
    event.isFree -> "Gratis"
    event.price != null -> event.price
    // Ticketmaster no siempre da el precio en la API, pero sí en la página del evento.
    event.url != null -> "Ver precio"
    // Lugares del catálogo sin tarifa fija: se paga en el sitio.
    else -> "En taquilla"
}

/** Degradado de respaldo por categoría mientras carga la imagen (tonos del prototipo). */
fun Category.fallbackColors(): List<Color> = when (this) {
    Category.Music -> listOf(Color(0xFF5A1A4A), Color(0xFF1A0F2A))
    Category.Markets -> listOf(Color(0xFF4A2A1A), Color(0xFF100B0A))
    Category.Fairs -> listOf(Color(0xFF3A2A1A), Color(0xFF100B0A))
    Category.Culture -> listOf(Color(0xFF3A1A5A), Color(0xFF0F0F2A))
    Category.Food -> listOf(Color(0xFF5A3A1A), Color(0xFF1A100B))
    Category.Art -> listOf(Color(0xFF1A3A5A), Color(0xFF0B0A10))
}
