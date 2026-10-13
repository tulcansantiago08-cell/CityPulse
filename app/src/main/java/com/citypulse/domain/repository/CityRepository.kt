package com.citypulse.domain.repository

import com.citypulse.domain.model.City

interface CityRepository {
    /** Ciudades populares que se ofrecen en el selector. */
    fun popularCities(): List<City>

    /** Búsqueda por nombre o país, sin distinguir mayúsculas ni acentos. */
    fun search(query: String): List<City>
}
