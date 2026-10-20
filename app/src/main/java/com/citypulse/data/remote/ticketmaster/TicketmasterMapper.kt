package com.citypulse.data.remote.ticketmaster

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Currency
import java.util.Locale

/** Duración supuesta cuando Ticketmaster no indica la hora de fin. */
private const val DEFAULT_DURATION_HOURS = 2L

/**
 * Convierte un evento de Ticketmaster al modelo de la app. Devuelve `null` si le falta algo
 * imprescindible (fecha o coordenadas del recinto).
 */
fun TmEvent.toDomain(): Event? {
    val venue = embedded?.venues?.firstOrNull() ?: return null
    val latitude = venue.location?.latitude?.toDoubleOrNull() ?: return null
    val longitude = venue.location.longitude?.toDoubleOrNull() ?: return null
    val startDate = dates?.start?.localDate?.let(LocalDate::parse) ?: return null

    // Sin hora concreta (ferias, exposiciones…) se asume horario de día.
    val allDay = dates.start.noSpecificTime || dates.start.localTime == null
    val start = if (allDay) startDate.atTime(10, 0) else startDate.atTime(LocalTime.parse(dates.start.localTime))
    val end = dates.end?.let { it.localDate?.let(LocalDate::parse)?.atTime(it.localTime?.let(LocalTime::parse) ?: LocalTime.of(20, 0)) }
        ?.takeIf { it.isAfter(start) }
        ?: if (allDay) startDate.atTime(20, 0) else start.plusHours(DEFAULT_DURATION_HOURS)

    val price = priceRanges.firstOrNull()
    val isFree = price != null && (price.max ?: price.min) == 0.0

    return Event(
        id = "tm_$id",
        title = name,
        category = classifications.firstOrNull().toCategory(),
        venue = venue.name ?: venue.city?.name.orEmpty(),
        area = venue.address?.line1 ?: venue.city?.name.orEmpty(),
        location = GeoPoint(latitude, longitude),
        start = start,
        end = end,
        price = price?.takeUnless { isFree }?.label(),
        isFree = isFree,
        description = info ?: pleaseNote ?: "Más información y entradas en Ticketmaster.",
        imageUrl = bestImage(),
        source = EventSource.Ticketmaster,
        url = url,
        priceMin = if (isFree) 0.0 else price?.let { it.min ?: it.max },
        currency = price?.currency,
    )
}

private fun TmClassification?.toCategory(): Category {
    val segment = this?.segment?.name.orEmpty()
    val genre = this?.genre?.name.orEmpty()
    return when {
        segment.equals("Music", ignoreCase = true) -> Category.Music
        genre.contains("Fine Art", ignoreCase = true) || genre.contains("Arts", ignoreCase = true) -> Category.Art
        genre.contains("Fair", ignoreCase = true) || genre.contains("Festival", ignoreCase = true) -> Category.Fairs
        genre.contains("Food", ignoreCase = true) -> Category.Food
        else -> Category.Culture
    }
}

private fun TmPriceRange.label(): String? {
    val amount = min ?: max ?: return null
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
        runCatching { currency = Currency.getInstance(this@label.currency ?: "EUR") }
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    val text = format.format(amount)
    return if (max != null && max != min) "Desde $text" else text
}

/** La imagen 16:9 más grande, que es la que mejor llena las tarjetas. */
private fun TmEvent.bestImage(): String? =
    (images.filter { it.ratio == "16_9" }.ifEmpty { images }).maxByOrNull { it.width }?.url

/** Formato que exige la API para startDateTime/endDateTime (UTC, sin milisegundos). */
fun LocalDateTime.toTicketmasterUtc(zone: java.time.ZoneId): String =
    atZone(zone).withZoneSameInstant(java.time.ZoneOffset.UTC)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
