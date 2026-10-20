package com.citypulse.data.remote.firestore

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.parsePriceLabel
import java.time.LocalDate
import java.time.LocalTime

/**
 * Entrada del catálogo propio (colección `events` de Firestore).
 *
 * Puede ser de una fecha concreta ([date]) o recurrente ([weekdays], 1 = lunes … 7 = domingo),
 * como los mercados de los domingos. Un [endTime] anterior a [startTime] termina al día siguiente.
 */
data class CatalogEntry(
    val id: String,
    val city: String,
    val title: String,
    val category: Category,
    val venue: String,
    val area: String,
    val location: GeoPoint,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val date: LocalDate?,
    val weekdays: Set<Int>,
    val price: String?,
    val isFree: Boolean,
    val description: String,
    val imageUrl: String?,
    val url: String?,
    val priceMin: Double? = null,
    val currency: String? = null,
) {
    /** Ocurrencias de la entrada entre [from] y los [days] días siguientes. */
    fun occurrences(from: LocalDate, days: Int = 7): List<Event> =
        (0 until days).map { from.plusDays(it.toLong()) }
            .filter { day -> if (date != null) day == date else day.dayOfWeek.value in weekdays }
            .map { day -> toEvent(day) }

    private fun toEvent(day: LocalDate): Event {
        val start = day.atTime(startTime)
        val end = if (endTime.isAfter(startTime)) day.atTime(endTime) else day.plusDays(1).atTime(endTime)
        return Event(
            id = "cp_${id}_$day",
            title = title,
            category = category,
            venue = venue,
            area = area,
            location = location,
            start = start,
            end = end,
            price = price,
            isFree = isFree,
            description = description,
            imageUrl = imageUrl,
            source = EventSource.CityPulse,
            url = url,
            priceMin = if (isFree) 0.0 else priceMin,
            currency = currency,
        )
    }

    companion object {
        /** Lee un documento de Firestore; devuelve `null` si le falta algún campo obligatorio. */
        fun fromDocument(id: String, data: Map<String, Any?>): CatalogEntry? = runCatching {
            // Si el documento no trae el precio en número, se deduce del texto ("15 €").
            val parsed = parsePriceLabel(data["price"] as? String)
            CatalogEntry(
                id = id,
                city = data["city"] as String,
                title = data["title"] as String,
                category = Category.valueOf(data["category"] as String),
                venue = data["venue"] as String,
                area = data["area"] as? String ?: "",
                location = GeoPoint((data["lat"] as Number).toDouble(), (data["lng"] as Number).toDouble()),
                startTime = LocalTime.parse(data["startTime"] as String),
                endTime = LocalTime.parse(data["endTime"] as String),
                date = (data["date"] as? String)?.let(LocalDate::parse),
                weekdays = (data["weekdays"] as? List<*>).orEmpty().map { (it as Number).toInt() }.toSet(),
                price = data["price"] as? String,
                isFree = data["free"] as? Boolean ?: false,
                description = data["description"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String,
                url = data["url"] as? String,
                priceMin = (data["priceMin"] as? Number)?.toDouble() ?: parsed?.first,
                currency = data["currency"] as? String ?: parsed?.second,
            )
        }.getOrNull()
    }
}
