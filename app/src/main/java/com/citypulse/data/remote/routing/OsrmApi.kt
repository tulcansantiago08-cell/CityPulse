package com.citypulse.data.remote.routing

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Servidores de rutas OSRM de FOSSGIS, uno por perfil (datos de OpenStreetMap).
 * Gratuitos y sin clave; sus normas piden identificar la app y no pasar de una petición por segundo.
 */
interface OsrmApi {
    /**
     * [server]: "routed-foot", "routed-bike" o "routed-car". [coordinates]: "lng,lat;lng,lat"
     * (origen y destino). Cada servidor tiene un único perfil, que se llama "driving" en la URL.
     */
    @GET("{server}/route/v1/driving/{coordinates}")
    suspend fun route(
        @Path("server") server: String,
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
