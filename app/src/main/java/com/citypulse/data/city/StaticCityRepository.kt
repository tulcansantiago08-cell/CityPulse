package com.citypulse.data.city

import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.text.normalizedForSearch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Lista fija de ciudades populares (las del prototipo) con las coordenadas de su centro. */
@Singleton
class StaticCityRepository @Inject constructor() : CityRepository {

    private val cities = listOf(
        city("Madrid", "España", 40.4168, -3.7038, "Europe/Madrid"),
        city("Barcelona", "España", 41.3874, 2.1686, "Europe/Madrid"),
        city("Valencia", "España", 39.4699, -0.3763, "Europe/Madrid"),
        city("Sevilla", "España", 37.3891, -5.9845, "Europe/Madrid"),
        city("Bilbao", "España", 43.2630, -2.9350, "Europe/Madrid"),
        city("Lisboa", "Portugal", 38.7223, -9.1393, "Europe/Lisbon"),
        city("Oporto", "Portugal", 41.1579, -8.6291, "Europe/Lisbon"),
        city("París", "Francia", 48.8566, 2.3522, "Europe/Paris"),
        city("Berlín", "Alemania", 52.5200, 13.4050, "Europe/Berlin"),
        city("Roma", "Italia", 41.9028, 12.4964, "Europe/Rome"),
        city("Ciudad de México", "México", 19.4326, -99.1332, "America/Mexico_City"),
        city("Buenos Aires", "Argentina", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        city("Bogotá", "Colombia", 4.7110, -74.0721, "America/Bogota"),
        city("Pasto", "Colombia", 1.2136, -77.2811, "America/Bogota"),
    )

    private fun city(name: String, country: String, lat: Double, lng: Double, zone: String) =
        City(name, country, GeoPoint(lat, lng), ZoneId.of(zone))

    override fun popularCities(): List<City> = cities

    override fun search(query: String): List<City> {
        val q = query.trim().normalizedForSearch()
        if (q.isEmpty()) return cities
        return cities.filter { q in it.name.normalizedForSearch() || q in it.country.normalizedForSearch() }
    }
}

