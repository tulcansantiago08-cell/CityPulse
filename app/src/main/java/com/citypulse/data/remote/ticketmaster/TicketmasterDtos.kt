package com.citypulse.data.remote.ticketmaster

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Subconjunto de la respuesta de Discovery API v2 que usa la app. */
@Serializable
data class TmEventsResponse(
    @SerialName("_embedded") val embedded: TmEmbeddedEvents? = null,
)

@Serializable
data class TmEmbeddedEvents(val events: List<TmEvent> = emptyList())

@Serializable
data class TmEvent(
    val id: String,
    val name: String,
    val url: String? = null,
    val info: String? = null,
    val pleaseNote: String? = null,
    val images: List<TmImage> = emptyList(),
    val dates: TmDates? = null,
    val classifications: List<TmClassification> = emptyList(),
    val priceRanges: List<TmPriceRange> = emptyList(),
    @SerialName("_embedded") val embedded: TmEmbeddedVenues? = null,
)

@Serializable
data class TmImage(val url: String, val width: Int = 0, val height: Int = 0, val ratio: String? = null)

@Serializable
data class TmDates(val start: TmDate? = null, val end: TmDate? = null)

@Serializable
data class TmDate(
    val localDate: String? = null,
    val localTime: String? = null,
    val noSpecificTime: Boolean = false,
)

@Serializable
data class TmClassification(val segment: TmNamed? = null, val genre: TmNamed? = null)

@Serializable
data class TmNamed(val name: String? = null)

@Serializable
data class TmPriceRange(val min: Double? = null, val max: Double? = null, val currency: String? = null)

@Serializable
data class TmEmbeddedVenues(val venues: List<TmVenue> = emptyList())

@Serializable
data class TmVenue(
    val name: String? = null,
    val city: TmNamed? = null,
    val address: TmAddress? = null,
    val location: TmLocation? = null,
)

@Serializable
data class TmAddress(val line1: String? = null)

@Serializable
data class TmLocation(val latitude: String? = null, val longitude: String? = null)
