package com.citypulse.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.citypulse.domain.text.normalizedForSearch
import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.City
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.repository.CityRepository
import com.citypulse.domain.repository.LocationRepository
import android.os.Looper
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
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
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) { freshOrLastLocation() }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Primero la posición actual y luego actualizaciones al moverse (cada minuto o 50 m como mucho).
     * Todas las pantallas comparten una sola suscripción al GPS, que se corta 5 s después de que
     * ninguna la use.
     */
    private val sharedLocation: Flow<GeoPoint?> by lazy {
        locationUpdates().shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
    }

    override fun observeLocation(): Flow<GeoPoint?> = sharedLocation

    @SuppressLint("MissingPermission") // Se comprueba en hasPermission().
    private fun locationUpdates(): Flow<GeoPoint?> = callbackFlow {
        send(currentLocation())
        if (!hasPermission()) {
            awaitClose()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(GeoPoint(it.latitude, it.longitude)) }
            }
        }
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fused.removeLocationUpdates(callback) }
    }.distinctUntilChanged()

    /**
     * Primero una posición reciente (de menos de 2 min) o nueva: quien viaja acaba de cambiar de
     * ciudad y la "última conocida" puede ser de otra. Si no llega a tiempo, se usa la última.
     */
    @SuppressLint("MissingPermission") // Se comprueba en currentLocation().
    private suspend fun freshOrLastLocation(): GeoPoint? {
        val fresh = withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS) {
            runCatching {
                val cancellation = CancellationTokenSource()
                // Con permiso preciso se usa el GPS: la distancia a pie necesita precisión.
                val priority = if (isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Priority.PRIORITY_HIGH_ACCURACY
                } else {
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY
                }
                val request = CurrentLocationRequest.Builder()
                    .setPriority(priority)
                    .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
                    .build()
                try {
                    fused.getCurrentLocation(request, cancellation.token).await()
                } finally {
                    cancellation.cancel()
                }
            }.getOrNull()
        }
        val location = fresh ?: runCatching { fused.lastLocation.await() }.getOrNull()
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    override suspend fun cityAt(point: GeoPoint): City? {
        val address = if (Geocoder.isPresent()) withTimeoutOrNull(LOCATION_TIMEOUT_MS) { reverseGeocode(point) } else null
        val name = address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            ?: return nearestKnownCity(point)
        // Si es una ciudad conocida se usa su centro; si no, el punto del usuario. Se admite que el
        // nombre oficial sea más largo ("San Juan de Pasto" es "Pasto").
        val geocoded = name.normalizedForSearch()
        val known = cityRepository.popularCities().firstOrNull {
            val candidate = it.name.normalizedForSearch()
            geocoded == candidate || geocoded.endsWith(" $candidate")
        }
        // El usuario está ahí: la zona horaria del teléfono es la del lugar.
        return known ?: City(name, address.countryName.orEmpty(), point, ZoneId.systemDefault(), address.countryCode)
    }

    override suspend fun searchPlaces(query: String): List<City> {
        if (query.isBlank() || !Geocoder.isPresent()) return emptyList()
        val addresses = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { geocodeName(query.trim()) }.orEmpty()
        return addresses
            .filter { it.hasLatitude() && it.hasLongitude() }
            .mapNotNull { address ->
                val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName ?: return@mapNotNull null
                City(
                    name = name,
                    country = address.countryName.orEmpty(),
                    center = GeoPoint(address.latitude, address.longitude),
                    zone = zoneForPlace(address.countryCode, name, address.longitude),
                    countryCode = address.countryCode,
                )
            }
            .distinctBy { "${it.name}|${it.country}" }
    }

    private suspend fun geocodeName(query: String): List<Address> {
        val geocoder = Geocoder(context, Locale.forLanguageTag("es"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(query, MAX_PLACE_RESULTS, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.toList())
                    override fun onError(errorMessage: String?) = cont.resume(emptyList())
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocationName(query, MAX_PLACE_RESULTS).orEmpty() }.getOrDefault(emptyList())
            }
        }
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
        const val FRESH_LOCATION_TIMEOUT_MS = 7_000L
        const val MAX_LOCATION_AGE_MS = 2 * 60 * 1000L
        const val NEAREST_CITY_MAX_METERS = 50_000.0
        const val MAX_PLACE_RESULTS = 5
        const val UPDATE_INTERVAL_MS = 60_000L
        const val MIN_UPDATE_DISTANCE_METERS = 50f
    }
}
