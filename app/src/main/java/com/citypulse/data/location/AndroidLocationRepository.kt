package com.citypulse.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.citypulse.data.city.normalizedForSearch
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Ubicación con Fused Location Provider y nombre de ciudad con el Geocoder del sistema. */
@Singleton
class AndroidLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cityRepository: CityRepository,
) : LocationRepository {

    private val fused = LocationServices.getFusedLocationProviderClient(context)

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasPermission(): Boolean =
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION) || isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission") // Se comprueba en hasPermission().
    override suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        // Límite total: si Play Services no responde (p. ej. mientras se actualiza), la app sigue
        // con el centro de la ciudad en vez de quedarse esperando para siempre.
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) { lastOrCurrentLocation() }
    }

    @SuppressLint("MissingPermission") // Se comprueba en currentLocation().
    private suspend fun lastOrCurrentLocation(): GeoPoint? {
        return runCatching {
            val location = fused.lastLocation.await()
                ?: run {
                    val cancellation = CancellationTokenSource()
                    // Con permiso preciso se usa el GPS: la distancia a pie necesita precisión.
                    val priority = if (isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Priority.PRIORITY_HIGH_ACCURACY
                    } else {
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                    try {
                        fused.getCurrentLocation(priority, cancellation.token).await()
                    } finally {
                        cancellation.cancel()
                    }
                }
            location?.let { GeoPoint(it.latitude, it.longitude) }
        }.getOrNull()
    }

    override suspend fun cityAt(point: GeoPoint): City? {
        val address = if (Geocoder.isPresent()) withTimeoutOrNull(LOCATION_TIMEOUT_MS) { reverseGeocode(point) } else null
        val name = address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            ?: return nearestKnownCity(point)
        // Si es una ciudad conocida se usa su centro; si no, el punto del usuario.
        val known = cityRepository.popularCities().firstOrNull { it.name.normalizedForSearch() == name.normalizedForSearch() }
        return known ?: City(name, address.countryName.orEmpty(), point)
    }

    /** Sin Geocoder (o sin red) se usa la ciudad conocida más cercana, si está a menos de 50 km. */
    private fun nearestKnownCity(point: GeoPoint): City? =
        cityRepository.popularCities()
            .minByOrNull { distanceMeters(point, it.center) }
            ?.takeIf { distanceMeters(point, it.center) <= NEAREST_CITY_MAX_METERS }

    private suspend fun reverseGeocode(point: GeoPoint): Address? {
        val geocoder = Geocoder(context, Locale.forLanguageTag("es"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(point.latitude, point.longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                    override fun onError(errorMessage: String?) = cont.resume(null)
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull() }.getOrNull()
            }
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 10_000L
        const val NEAREST_CITY_MAX_METERS = 50_000.0
    }
}
