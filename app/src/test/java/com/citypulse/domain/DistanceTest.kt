package com.citypulse.domain

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.geo.walkingMinutes
import com.citypulse.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {

    @Test
    fun `haversine entre Sol y Plaza Mayor es de unos 360 metros`() {
        val plazaMayor = GeoPoint(40.4155, -3.7074)
        assertEquals(360.0, distanceMeters(PuertaDelSol, plazaMayor), 40.0)
    }

    @Test
    fun `minutos a pie aplican el factor de calles y nunca bajan de 1`() {
        assertEquals(1, walkingMinutes(0.0))
        assertEquals(13, walkingMinutes(800.0)) // 800 m × 1,3 / 80 m/min
    }
}
