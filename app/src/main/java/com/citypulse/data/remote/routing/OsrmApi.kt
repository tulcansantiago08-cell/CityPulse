package com.citypulse.data.remote.routing

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Servidor de rutas OSRM de FOSSGIS con el perfil a pie (datos de OpenStreetMap).
 * Gratuito y sin clave; sus normas piden identificar la app y no pasar de una petición por segundo.
 */
interface OsrmApi {
    /** [coordinates]: "lng,lat;lng,lat" (origen y destino). */
    @GET("routed-foot/route/v1/foot/{coordinates}")
    suspend fun route(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("alternatives") alternatives: Boolean = false,
        @Query("steps") steps: Boolean = false,
    ): OsrmResponse

    companion object {
        const val BASE_URL = "https://routing.openstreetmap.de/"
    }
}
