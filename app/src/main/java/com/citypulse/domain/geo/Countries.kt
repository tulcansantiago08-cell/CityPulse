package com.citypulse.domain.geo

import com.citypulse.domain.text.normalizedForSearch
import java.util.Currency
import java.util.Locale

/**
 * Código ISO del país a partir de su nombre en español o en inglés ("España", "Japan", "Perú").
 * Sirve para ciudades guardadas sin código; `null` si no se reconoce.
 */
fun countryCodeFor(countryName: String): String? {
    val wanted = countryName.trim().normalizedForSearch()
    if (wanted.isEmpty()) return null
    return Locale.getISOCountries().firstOrNull { code ->
        val country = Locale("", code)
        country.getDisplayCountry(SPANISH).normalizedForSearch() == wanted ||
            country.getDisplayCountry(Locale.ENGLISH).normalizedForSearch() == wanted
    }
}

/** Moneda local de un país (ISO 4217); dólar estadounidense si no se conoce. */
fun currencyForCountry(countryCode: String?): String =
    countryCode
        ?.let { runCatching { Currency.getInstance(Locale("", it.uppercase()))?.currencyCode }.getOrNull() }
        ?: "USD"

private val SPANISH = Locale.forLanguageTag("es")
