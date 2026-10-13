package com.citypulse.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.GeoPoint
import java.time.LocalDateTime

/** Copia completa del evento para poder mostrarlo sin conexión. */
@Entity(tableName = "saved_events")
data class SavedEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val venue: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val start: String,
    val end: String,
    val price: String?,
    val description: String,
    val imageUrl: String?,
    val source: String,
    val url: String?,
    val savedAt: Long,
)

fun Event.toSavedEntity(savedAt: Long = System.currentTimeMillis()) = SavedEventEntity(
    id = id,
    title = title,
    category = category.name,
    venue = venue,
    area = area,
    latitude = location.latitude,
    longitude = location.longitude,
    start = start.toString(),
    end = end.toString(),
    price = price,
    description = description,
    imageUrl = imageUrl,
    source = source.name,
    url = url,
    savedAt = savedAt,
)

fun SavedEventEntity.toEvent() = Event(
    id = id,
    title = title,
    category = Category.valueOf(category),
    venue = venue,
    area = area,
    location = GeoPoint(latitude, longitude),
    start = LocalDateTime.parse(start),
    end = LocalDateTime.parse(end),
    price = price,
    description = description,
    imageUrl = imageUrl,
    source = EventSource.valueOf(source),
    url = url,
)
