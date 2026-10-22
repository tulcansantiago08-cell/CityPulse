package com.citypulse.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.PriceEstimate
import java.time.LocalDateTime

/** Última lista de eventos descargada por ciudad, para mostrar algo al instante y sin conexión. */
@Entity(tableName = "cached_events", indices = [Index("city")])
data class CachedEventEntity(
    @PrimaryKey val id: String,
    val city: String,
    val title: String,
    val category: String,
    val venue: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val start: String,
    val end: String,
    val price: String?,
    val free: Boolean,
    val description: String,
    val imageUrl: String?,
    val source: String,
    val url: String?,
    val priceMin: Double? = null,
    val currency: String? = null,
    /** "Local" o "Ai" si el precio es estimado (ver [PriceEstimate]). */
    val priceEstimate: String? = null,
    val fetchedAt: Long,
)

fun Event.toCachedEntity(city: String, fetchedAt: Long) = CachedEventEntity(
    id = id,
    city = city,
    title = title,
    category = category.name,
    venue = venue,
    area = area,
    latitude = location.latitude,
    longitude = location.longitude,
    start = start.toString(),
    end = end.toString(),
    price = price,
    free = isFree,
    description = description,
    imageUrl = imageUrl,
    source = source.name,
    url = url,
    priceMin = priceMin,
    currency = currency,
    priceEstimate = priceEstimate?.name,
    fetchedAt = fetchedAt,
)

fun CachedEventEntity.toEvent() = Event(
    id = id,
    title = title,
    category = Category.valueOf(category),
    venue = venue,
    area = area,
    location = GeoPoint(latitude, longitude),
    start = LocalDateTime.parse(start),
    end = LocalDateTime.parse(end),
    price = price,
    isFree = free,
    description = description,
    imageUrl = imageUrl,
    source = EventSource.valueOf(source),
    url = url,
    priceMin = priceMin,
    currency = currency,
    priceEstimate = priceEstimate?.let { runCatching { PriceEstimate.valueOf(it) }.getOrNull() },
)
