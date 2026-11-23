package com.citypulse.ui.common

import androidx.compose.ui.graphics.Color
import com.citypulse.domain.i18n.AppLocale
import com.citypulse.domain.i18n.tr
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.PriceEstimate
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
    /** "Gratis", el precio, "≈ 25 €" si es estimado, "Ver precio" (está en la web del evento) o "En taquilla". */
    val priceLabel: String,
    /** El precio es una estimación (la fuente no lo publica); `null` si es real o no hay. */
    val priceEstimate: PriceEstimate? = null,
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
private val EnglishDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val EnglishDaysLong = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val EnglishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Nombres de días y meses en el idioma de la app. */
private val days get() = if (AppLocale.isEnglish) EnglishDays else SpanishDays
private val daysLong get() = if (AppLocale.isEnglish) EnglishDaysLong else SpanishDaysLong
private val months get() = if (AppLocale.isEnglish) EnglishMonths else SpanishMonths

fun LocalDateTime.hourLabel(): String = format(HourFormat)

/** "Hoy", "Mañana" o "Mié 14". */
fun LocalDate.dayLabel(today: LocalDate): String = when (this) {
    today -> tr("Hoy", "Today")
    today.plusDays(1) -> tr("Mañana", "Tomorrow")
    else -> "${days[dayOfWeek.value - 1]} $dayOfMonth"
}

/** Línea de fecha de la pantalla de inicio: "Martes 13 oct · 21:04". */
fun LocalDateTime.dateLine(): String =
    if (AppLocale.isEnglish) {
        "${daysLong[dayOfWeek.value - 1]}, ${months[monthValue - 1]} $dayOfMonth · ${hourLabel()}"
    } else {
        "${daysLong[dayOfWeek.value - 1]} $dayOfMonth ${months[monthValue - 1]} · ${hourLabel()}"
    }

/** Fecha absoluta para compartir: "martes 20 oct · 21:00" / "Tuesday, Oct 20 · 21:00". */
private fun LocalDateTime.absoluteLabel(): String = if (AppLocale.isEnglish) {
    "${daysLong[dayOfWeek.value - 1]}, ${months[monthValue - 1]} $dayOfMonth · ${hourLabel()}"
} else {
    "${daysLong[dayOfWeek.value - 1].lowercase()} $dayOfMonth ${months[monthValue - 1]} · ${hourLabel()}"
}

/** Importe redondo con su moneda: "10 €", "20.000 COP". */
fun formatMoney(amount: Double, currency: String): String =
    NumberFormat.getCurrencyInstance(AppLocale.current).apply {
        runCatching { this.currency = Currency.getInstance(currency) }
        maximumFractionDigits = 0
    }.format(amount)

fun distanceLabel(meters: Int, walkMinutes: Int): String {
    val km = String.format(AppLocale.current, "%.1f", meters / 1000.0)
    return "$km km · $walkMinutes " + tr("min a pie", "min walk")
}

fun NearbyEvent.toUi(now: LocalDateTime): EventUi {
    val live = event.isLive(now)
    val overline = if (live) tr("hasta ", "until ") + event.end.hourLabel() else tr("empieza ", "starts ") + event.start.hourLabel()
    return EventUi(
        id = event.id,
        title = event.title,
        categoryLabel = event.category.displayLabel,
        venue = event.venue,
        area = event.area,
        imageUrl = event.imageUrl,
        fallback = event.category.fallbackColors(),
        live = live,
        free = event.isFree,
        priceLabel = priceLabel(event),
        priceEstimate = event.priceEstimate.takeIf { !event.isFree && event.priceMin != null },
        priceOnline = !event.isFree && event.price == null && event.url != null,
        ticketsUrl = when {
            event.isFree -> null
            event.url != null -> event.url
            else -> ticketSearchUrl(event)
        },
        walkMinutes = walkMinutes,
        overline = "${event.category.displayLabel} · $overline",
        whenLabel = "${event.start.toLocalDate().dayLabel(now.toLocalDate())} · ${event.start.hourLabel()}",
        absoluteWhen = event.start.absoluteLabel(),
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
    val query = URLEncoder.encode("${event.title} ${event.venue} ${event.area} " + tr("entradas", "tickets"), Charsets.UTF_8.name())
    return "https://www.google.com/search?q=$query&hl=${AppLocale.language.tag}"
}

private fun priceLabel(event: Event): String = when {
    event.isFree -> tr("Gratis", "Free")
    event.price != null -> event.price
    // Precio estimado por la IA o por la regla local: se marca con "≈" para no confundirlo con el real.
    event.priceEstimate != null && event.priceMin != null -> "≈ " + formatMoney(event.priceMin, event.currency ?: "EUR")
    // Ticketmaster no siempre da el precio en la API, pero sí en la página del evento.
    event.url != null -> tr("Ver precio", "See price")
    // Lugares del catálogo sin tarifa fija: se paga en el sitio.
    else -> tr("En taquilla", "At the door")
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
