package com.citypulse.data.city

import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.CityRepository
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/** Lista fija de ciudades populares (las del prototipo) con las coordenadas de su centro. */
@Singleton
class StaticCityRepository @Inject constructor() : CityRepository {

    private val cities = listOf(
        City("Madrid", "España", GeoPoint(40.4168, -3.7038)),
        City("Barcelona", "España", GeoPoint(41.3874, 2.1686)),
        City("Valencia", "España", GeoPoint(39.4699, -0.3763)),
        City("Sevilla", "España", GeoPoint(37.3891, -5.9845)),
        City("Bilbao", "España", GeoPoint(43.2630, -2.9350)),
        City("Lisboa", "Portugal", GeoPoint(38.7223, -9.1393)),
        City("Oporto", "Portugal", GeoPoint(41.1579, -8.6291)),
        City("París", "Francia", GeoPoint(48.8566, 2.3522)),
        City("Berlín", "Alemania", GeoPoint(52.5200, 13.4050)),
        City("Roma", "Italia", GeoPoint(41.9028, 12.4964)),
        City("Ciudad de México", "México", GeoPoint(19.4326, -99.1332)),
        City("Buenos Aires", "Argentina", GeoPoint(-34.6037, -58.3816)),
        City("Bogotá", "Colombia", GeoPoint(4.7110, -74.0721)),
    )

    override fun popularCities(): List<City> = cities

    override fun search(query: String): List<City> {
        val q = query.trim().normalizedForSearch()
        if (q.isEmpty()) return cities
        return cities.filter { q in it.name.normalizedForSearch() || q in it.country.normalizedForSearch() }
    }
}

/** Minúsculas y sin tildes: "Bogotá" → "bogota". */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
