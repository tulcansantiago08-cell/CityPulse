package com.citypulse.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Empieza la navegación a pie de Google Maps hasta el evento, desde la ubicación actual y sin pasos
 * intermedios (no hay que buscar el destino ni pulsar "Iniciar").
 *
 * Con la app de Google Maps instalada usa su enlace de navegación (`google.navigation:`, modo a
 * pie). Sin ella abre la web de Maps pidiendo también que empiece a navegar (`dir_action=navigate`).
 */
fun Context.openWalkingDirections(latitude: Double, longitude: Double) {
    val navigation = Intent(Intent.ACTION_VIEW, googleMapsNavigationUri(latitude, longitude))
        .setPackage(GOOGLE_MAPS_PACKAGE)
    try {
        startActivity(navigation)
        return
    } catch (_: ActivityNotFoundException) {
        // Sin la app de Google Maps: se sigue con la web.
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, googleMapsWebDirectionsUri(latitude, longitude)))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador ni app de mapas no hay nada que abrir.
    }
}

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

/** Navegación paso a paso en la app de Google Maps; `mode=w` es a pie. */
internal fun googleMapsNavigationUri(latitude: Double, longitude: Double) =
    "google.navigation:q=$latitude,$longitude&mode=w".toUri()

/** Ruta a pie en la web de Google Maps, empezando a navegar si el navegador lo permite (hl=es: en español). */
internal fun googleMapsWebDirectionsUri(latitude: Double, longitude: Double) =
    "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking&dir_action=navigate&hl=es".toUri()

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
