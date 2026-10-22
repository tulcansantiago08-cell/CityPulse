package com.citypulse.data.location

import com.citypulse.domain.text.normalizedForSearch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/** Diferencia máxima (s) entre la hora estándar de una zona y la solar del lugar para considerarla. */
private const val OFFSET_TOLERANCE_SECONDS = 30 * 60

/**
 * Zona horaria de un lugar a partir de su país, su nombre y su longitud, sin conexión. En países
 * con una sola zona es exacta. En los que tienen varias (EE. UU., Canadá, México, Brasil,
 * Australia…):
 * 1. Si alguna zona lleva el nombre del lugar (America/Toronto, America/Phoenix), esa.
 * 2. Si no, las de hora estándar más parecida a la solar del lugar (15° de longitud por hora) y,
 *    entre ellas, una con horario de verano si la hay, que es lo habitual en la mayoría del país.
 * Si no se conoce el país, se usa la del teléfono.
 */
fun zoneForPlace(
    countryCode: String?,
    placeName: String,
    longitude: Double,
    fallback: ZoneId = ZoneId.systemDefault(),
): ZoneId {
    val region = countryCode?.uppercase() ?: return fallback
    val ids = android.icu.util.TimeZone.getAvailableIDs(region)
        .mapNotNull { runCatching { ZoneId.of(it) }.getOrNull() }
    if (ids.isEmpty()) return fallback
    if (ids.size == 1) return ids.first()

    val place = placeName.normalizedForSearch().replace(' ', '_')
    ids.firstOrNull { it.id.substringAfterLast('/').normalizedForSearch() == place }?.let { return it }

    val now = Instant.now()
    val solarOffsetSeconds = longitude / 15.0 * 3600
    val best = ids.minOf { abs(it.rules.getStandardOffset(now).totalSeconds - solarOffsetSeconds) }
    val candidates = ids.filter { abs(it.rules.getStandardOffset(now).totalSeconds - solarOffsetSeconds) <= best + OFFSET_TOLERANCE_SECONDS }
        .filter { abs(it.rules.getStandardOffset(now).totalSeconds - solarOffsetSeconds) - best < 1 }
    return candidates.firstOrNull { it.rules.nextTransition(now) != null } ?: candidates.first()
}
