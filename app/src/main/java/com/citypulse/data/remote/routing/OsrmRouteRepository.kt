package com.citypulse.data.remote.routing

import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.WalkingRoute
import com.citypulse.domain.repository.RouteRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Como mínimo este tiempo entre dos peticiones (las normas del servidor: 1 por segundo). */
private const val MIN_INTERVAL_MS = 1_100L

@Singleton
class OsrmRouteRepository @Inject constructor(
    private val api: OsrmApi,
) : RouteRepository {

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    override suspend fun walkingRoute(from: GeoPoint, to: GeoPoint): Result<WalkingRoute> = mutex.withLock {
        val wait = lastRequestAt + MIN_INTERVAL_MS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        lastRequestAt = System.currentTimeMillis()
        try {
            val route = api.route(coordinates(from, to)).toWalkingRoute()
            if (route != null) Result.success(route) else Result.failure(IllegalStateException("Sin ruta a pie"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun coordinates(from: GeoPoint, to: GeoPoint) = String.format(
        Locale.US,
        "%.6f,%.6f;%.6f,%.6f",
        from.longitude, from.latitude, to.longitude, to.latitude,
    )
}
