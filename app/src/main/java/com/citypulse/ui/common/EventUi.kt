package com.citypulse.ui.common

import androidx.compose.ui.graphics.Color
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.NearbyEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    val walkMinutes: Int,
    /** "Música · hasta 23:30" / "Música · empieza 22:00" */
    val overline: String,
    /** "Hoy · 21:00", "Mié 14 · 08:00" */
    val whenLabel: String,
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
        walkMinutes = walkMinutes,
        overline = "${event.category.label} · $overline",
        whenLabel = "${event.start.toLocalDate().dayLabel(now.toLocalDate())} · ${event.start.hourLabel()}",
        timeRange = "${event.start.hourLabel()}–${event.end.hourLabel()}",
        distanceLabel = distanceLabel(distanceMeters, walkMinutes),
        description = event.description,
        latitude = event.location.latitude,
        longitude = event.location.longitude,
        url = event.url,
    )
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
