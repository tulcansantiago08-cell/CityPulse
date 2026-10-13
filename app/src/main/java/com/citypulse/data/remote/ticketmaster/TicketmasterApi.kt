package com.citypulse.data.remote.ticketmaster

import retrofit2.http.GET
import retrofit2.http.Query

/** Ticketmaster Discovery API v2. La clave se añade con un interceptor (ver NetworkModule). */
interface TicketmasterApi {

    @GET("discovery/v2/events.json")
    suspend fun searchEvents(
        @Query("latlong") latLong: String,
        @Query("radius") radiusKm: Int,
        @Query("unit") unit: String = "km",
        @Query("startDateTime") startDateTime: String,
        @Query("endDateTime") endDateTime: String,
        @Query("sort") sort: String = "date,asc",
        @Query("size") size: Int = 100,
        @Query("locale") locale: String = "*",
    ): TmEventsResponse

    companion object {
        const val BASE_URL = "https://app.ticketmaster.com/"
    }
}
