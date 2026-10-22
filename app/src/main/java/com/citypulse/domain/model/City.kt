package com.citypulse.domain.model

import com.citypulse.domain.geo.countryCodeFor
import com.citypulse.domain.geo.currencyForCountry
import java.time.ZoneId

/**
 * Ciudad o lugar donde se buscan eventos: puede ser cualquier sitio del mundo (por GPS o
 * buscándolo), no solo las ciudades de la lista. [center] es el punto alrededor del que se buscan
 * los eventos y la referencia cuando no hay ubicación del usuario. Los horarios de sus eventos
 * están en su zona horaria, [zone].
 *
 * @property countryCode código ISO del país ("ES", "CO", "JP"); si falta, se deduce de [country].
 */
data class City(
    val name: String,
    val country: String,
    val center: GeoPoint,
    val zone: ZoneId = ZoneId.systemDefault(),
    val countryCode: String? = null,
) {
    /** Moneda local (ISO 4217), para el presupuesto y los precios estimados. */
    val currency: String get() = currencyForCountry(countryCode ?: countryCodeFor(country))

    /** Identifica el lugar en la caché: hay ciudades con el mismo nombre en distintos países. */
    val key: String get() = "$name|${countryCode ?: countryCodeFor(country) ?: country}"
}
