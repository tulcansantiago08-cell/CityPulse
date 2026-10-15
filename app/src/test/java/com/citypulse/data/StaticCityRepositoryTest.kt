package com.citypulse.data

import com.citypulse.data.city.StaticCityRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticCityRepositoryTest {

    private val repository = StaticCityRepository()

    @Test
    fun `busqueda ignora tildes y mayusculas`() {
        assertEquals(listOf("Bogotá"), repository.search("BOGOTA").map { it.name })
        assertEquals(listOf("Pasto"), repository.search("pasto").map { it.name })
        assertEquals(listOf("París"), repository.search("paris").map { it.name })
    }

    @Test
    fun `busca tambien por pais`() {
        assertEquals(listOf("Lisboa", "Oporto"), repository.search("portugal").map { it.name })
        assertEquals(listOf("Bogotá", "Pasto"), repository.search("colombia").map { it.name })
    }

    @Test
    fun `consulta vacia devuelve todas y sin resultados devuelve lista vacia`() {
        assertEquals(repository.popularCities(), repository.search("  "))
        assertTrue(repository.search("atlantida").isEmpty())
    }
}
