package com.citypulse.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Abre Google Maps (app o web) con la ruta a pie hasta el evento. */
fun Context.openWalkingDirections(latitude: Double, longitude: Double) {
    val uri = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking".toUri()
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // Sin navegador ni app de mapas no hay nada que abrir.
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
    append("\n").append(whenLabel).append(" (").append(timeRange).append(")")
    url?.let { append("\n").append(it) }
}
