package com.citypulse.data.fake

import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Ubicación fija en la Puerta del Sol de Madrid. */
@Singleton
class FakeLocationRepository @Inject constructor() : LocationRepository {
    override suspend fun currentLocation(): GeoPoint = SampleData.PuertaDelSol
    override suspend fun cityAt(point: GeoPoint): City = SampleData.Madrid
}
