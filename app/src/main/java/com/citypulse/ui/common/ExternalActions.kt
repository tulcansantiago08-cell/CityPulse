package com.citypulse.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.citypulse.domain.model.TravelMode

/**
 * Abre Google Maps hacia el evento desde la ubicación actual, en el modo elegido: empieza la
 * navegación paso a paso sin pasos intermedios, salvo en transporte público, que no tiene enlace de
 * navegación y muestra las líneas y horarios. Sin la app de Google Maps se usa su web.
 *
 * Con [placeQuery] busca el lugar por su nombre, para que Maps lo muestre así; si no, usa las coordenadas.
 * Con [originQuery] muestra la ruta desde ese lugar en vez de navegar desde la ubicación actual.
 */
fun Context.openDirections(
    latitude: Double,
    longitude: Double,
    mode: TravelMode,
    placeQuery: String? = null,
    originQuery: String? = null,
) {
    val destination = placeQuery?.let(Uri::encode) ?: "$latitude,$longitude"
    val from = originQuery?.let(Uri::encode)
    val app = Intent(Intent.ACTION_VIEW, googleMapsAppUri(destination, mode, from)).setPackage(GOOGLE_MAPS_PACKAGE)
    try {
        startActivity(app)
        return
    } catch (_: ActivityNotFoundException) {
        // Sin la app de Google Maps: se sigue con la web.
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, googleMapsWebDirectionsUri(destination, mode, from)))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador ni app de mapas no hay nada que abrir.
    }
}

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

/** `google.navigation` admite a pie (w), bici (b) y carro (d); el transporte público va por la ruta. */
internal fun googleMapsAppUri(destination: String, mode: TravelMode, origin: String? = null) = when {
    origin != null -> "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$destination&travelmode=${mode.googleMapsName}"
    mode == TravelMode.WALK -> "google.navigation:q=$destination&mode=w"
    mode == TravelMode.BIKE -> "google.navigation:q=$destination&mode=b"
    mode == TravelMode.CAR -> "google.navigation:q=$destination&mode=d"
    else -> "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=transit"
}.toUri()

private val TravelMode.googleMapsName: String
    get() = when (this) {
        TravelMode.WALK -> "walking"
        TravelMode.BIKE -> "bicycling"
        TravelMode.CAR -> "driving"
        TravelMode.TRANSIT -> "transit"
    }

/** Ruta en la web de Google Maps, empezando a navegar si el navegador lo permite (hl=es: en español). */
internal fun googleMapsWebDirectionsUri(destination: String, mode: TravelMode, origin: String? = null): Uri {
    val from = origin?.let { "&origin=$it" }.orEmpty()
    val navigate = if (mode == TravelMode.TRANSIT || origin != null) "" else "&dir_action=navigate"
    return "https://www.google.com/maps/dir/?api=1$from&destination=$destination&travelmode=${mode.googleMapsName}$navigate&hl=es".toUri()
}

/** Abre una web (p. ej. la página del evento con precios y entradas). */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador no hay nada que abrir.
    }
}

/** Comparte un texto con el selector del sistema. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, null))
}

fun EventUi.shareMessage(): String = buildString {
    append(title)
    append(" — ").append(venue).append(", ").append(area)
    append("\n").append(absoluteWhen).append(" (").append(timeRange).append(")")
    url?.let { append("\n").append(it) }
}
