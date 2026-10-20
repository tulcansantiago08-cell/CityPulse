package com.citypulse.domain

import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import java.time.LocalDateTime

val PuertaDelSol = GeoPoint(40.4169, -3.7035)

fun testEvent(
    id: String = "e",
    start: LocalDateTime,
    end: LocalDateTime = start.plusHours(2),
    location: GeoPoint = PuertaDelSol,
    category: Category = Category.Music,
    price: String? = null,
    priceMin: Double? = null,
    currency: String? = "EUR",
) = Event(
    id = id,
    title = "Evento $id",
    category = category,
    venue = "Sala",
    area = "Centro",
    location = location,
    start = start,
    end = end,
    price = price,
    isFree = price == null,
    priceMin = if (price == null) 0.0 else priceMin,
    currency = currency,
    description = "",
    imageUrl = null,
    source = EventSource.CityPulse,
)
