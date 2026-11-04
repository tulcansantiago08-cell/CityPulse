package com.citypulse.data.remote

import com.citypulse.data.remote.routing.OsrmResponse
import com.citypulse.data.remote.routing.toTravelRoute
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TravelMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `convierte la ruta de OSRM a puntos con latitud y longitud en orden`() {
        // Respuesta real recortada: Plaza de Nariño → Museo Taminango (GeoJSON es [lng, lat]).
        val body = """
            {"code":"Ok","waypoints":[{"name":"Calle 19"}],
             "routes":[{"distance":812.4,"duration":584.9,"weight":584.9,
               "geometry":{"type":"LineString","coordinates":[[-77.27827,1.21471],[-77.28011,1.21503],[-77.28422,1.21528]]},
               "legs":[]}]}
        """.trimIndent()
        val route = json.decodeFromString<OsrmResponse>(body).toTravelRoute(TravelMode.WALK)!!
        assertEquals(GeoPoint(1.21471, -77.27827), route.points.first())
        assertEquals(GeoPoint(1.21528, -77.28422), route.points.last())
        assertEquals(812.4, route.distanceMeters, 0.0)
        // 812 m a 80 m/min, redondeado hacia arriba: 11 min (el mismo ritmo que el resto de la app).
        assertEquals(11, route.minutes)
        assertTrue(route.exact)
    }

    @Test
    fun `en carro usa la duracion de OSRM redondeada hacia arriba`() {
        val body = """{"code":"Ok","routes":[{"distance":2384.0,"duration":290.5,"geometry":{"coordinates":[[-77.27,1.21],[-77.28,1.22]]}}]}"""
        assertEquals(5, json.decodeFromString<OsrmResponse>(body).toTravelRoute(TravelMode.CAR)!!.minutes)
    }

    @Test
    fun `sin ruta devuelve null`() {
        assertNull(json.decodeFromString<OsrmResponse>("""{"code":"NoRoute","routes":[]}""").toTravelRoute(TravelMode.WALK))
        assertNull(json.decodeFromString<OsrmResponse>("""{"code":"Ok","routes":[]}""").toTravelRoute(TravelMode.WALK))
    }
}
