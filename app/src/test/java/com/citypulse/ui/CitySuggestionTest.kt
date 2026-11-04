package com.citypulse.ui

import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.usecase.SuggestCityChange
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CitySuggestionTest {
    private val pasto = City("Pasto", "Colombia", GeoPoint(1.2136, -77.2811), ZoneId.of("America/Bogota"), "CO")

    private class At(private val point: GeoPoint?, private val city: City?) : LocationRepository {
        override suspend fun currentLocation() = point
        override fun observeLocation(): Flow<GeoPoint?> = flowOf(point)
        override suspend fun cityAt(point: GeoPoint) = city
        override suspend fun searchPlaces(query: String) = emptyList<City>()
    }

    @Test
    fun `sugiere la ciudad del usuario si esta lejos de la elegida`() = runTest {
        val suggestion = SuggestCityChange(FakePreferences(), At(pasto.center, pasto))().first()
        assertEquals("Pasto", suggestion?.name)
    }

    @Test
    fun `no sugiere nada cerca de la elegida ni sin ubicacion`() = runTest {
        assertNull(SuggestCityChange(FakePreferences(), At(SampleData.PuertaDelSol, pasto))().first())
        assertNull(SuggestCityChange(FakePreferences(), At(null, pasto))().first())
    }

    @Test
    fun `no sugiere la misma ciudad ni si no se puede resolver`() = runTest {
        assertNull(SuggestCityChange(FakePreferences(), At(pasto.center, SampleData.Madrid))().first())
        assertNull(SuggestCityChange(FakePreferences(), At(pasto.center, null))().first())
    }
}
