package com.citypulse.data.remote.routing

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.NoRouteException
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import com.citypulse.domain.repository.RouteRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/** Como mínimo este tiempo entre dos peticiones (las normas del servidor: 1 por segundo). */
private const val MIN_INTERVAL_MS = 1_100L

/**
 * OSRM lleva cada punto a la carretera más cercana que tenga conectada, aunque esté a cientos de
 * kilómetros: una ruta que empieza o acaba más lejos que esto no llega al sitio pedido.
 */
private const val MAX_SNAP_METERS = 2_000.0

private const val HTTP_BAD_REQUEST = 400

@Singleton
class OsrmRouteRepository @Inject constructor(
    private val api: OsrmApi,
) : RouteRepository {

    private val mutex = Mutex()
    private var lastRequestAt = 0L

    override suspend fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): Result<TravelRoute> = mutex.withLock {
        val server = mode.server() ?: return@withLock Result.failure(NoRouteException())
        val wait = lastRequestAt + MIN_INTERVAL_MS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        lastRequestAt = System.currentTimeMillis()
        try {
            val route = api.route(server, coordinates(from, to)).toTravelRoute(mode)
            if (route != null && route.reaches(from, to)) Result.success(route) else Result.failure(NoRouteException())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // OSRM contesta 400 cuando no hay camino entre los puntos.
            Result.failure(if (e.code() == HTTP_BAD_REQUEST) NoRouteException() else e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TravelRoute.reaches(from: GeoPoint, to: GeoPoint) =
        distanceMeters(points.first(), from) <= MAX_SNAP_METERS && distanceMeters(points.last(), to) <= MAX_SNAP_METERS

    private fun TravelMode.server(): String? = when (this) {
        TravelMode.WALK -> "routed-foot"
        TravelMode.BIKE -> "routed-bike"
        TravelMode.CAR -> "routed-car"
        TravelMode.TRANSIT -> null
    }

    private fun coordinates(from: GeoPoint, to: GeoPoint) = String.format(
        Locale.US,
        "%.6f,%.6f;%.6f,%.6f",
        from.longitude, from.latitude, to.longitude, to.latitude,
    )
}
