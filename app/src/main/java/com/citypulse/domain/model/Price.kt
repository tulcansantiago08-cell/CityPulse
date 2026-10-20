package com.citypulse.domain.model

/**
 * Rango de precio del filtro de Inicio. Los límites dependen de la moneda de la ciudad
 * (ver [PriceBands]): "hasta 10" son euros en Madrid y "hasta 20.000" pesos en Bogotá.
 */
enum class PriceRange { Any, Low, Mid, High }

/** Límites entre rangos para una moneda: Low ≤ [low] < Mid ≤ [high] < High. */
data class PriceBands(val currency: String, val low: Double, val high: Double) {
    companion object {
        /** Moneda habitual del país de la ciudad (por nombre, como lo guarda [City]). */
        fun currencyFor(country: String): String = when (country.lowercase()) {
            "españa", "portugal", "francia", "alemania", "italia" -> "EUR"
            "colombia" -> "COP"
            "méxico", "mexico" -> "MXN"
            "argentina" -> "ARS"
            else -> "USD"
        }

        fun forCurrency(currency: String): PriceBands = when (currency) {
            "COP" -> PriceBands(currency, 20_000.0, 60_000.0)
            "MXN" -> PriceBands(currency, 200.0, 600.0)
            "ARS" -> PriceBands(currency, 10_000.0, 30_000.0)
            else -> PriceBands(currency, 10.0, 30.0) // EUR, USD y otras de valor parecido
        }

        fun forCity(city: City): PriceBands = forCurrency(currencyFor(city.country))
    }
}

/**
 * ¿Entra el evento en el rango? Los gratuitos cuentan como el más barato. Si no se conoce el
 * precio (o está en otra moneda) solo encaja en [PriceRange.Any].
 */
fun Event.isInPriceRange(range: PriceRange, bands: PriceBands): Boolean {
    if (range == PriceRange.Any) return true
    val amount = if (isFree) 0.0 else priceMin?.takeIf { currency == bands.currency } ?: return false
    return when (range) {
        PriceRange.Any -> true
        PriceRange.Low -> amount <= bands.low
        PriceRange.Mid -> amount > bands.low && amount <= bands.high
        PriceRange.High -> amount > bands.high
    }
}

/** Precio mínimo y moneda leídos de un texto como "15 €", "Desde 40.000 COP" o "$ 20,50". */
fun parsePriceLabel(label: String?): Pair<Double, String>? {
    if (label.isNullOrBlank()) return null
    val currency = when {
        "€" in label || "EUR" in label -> "EUR"
        "COP" in label -> "COP"
        "MXN" in label -> "MXN"
        "ARS" in label -> "ARS"
        "$" in label || "USD" in label -> "USD"
        else -> return null
    }
    // Formato español: punto para miles y coma para decimales.
    val number = Regex("""\d[\d.]*(,\d+)?""").find(label)?.value ?: return null
    val amount = number.replace(".", "").replace(",", ".").toDoubleOrNull() ?: return null
    return amount to currency
}
