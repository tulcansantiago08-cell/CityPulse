package com.citypulse.domain.model

/**
 * Escala del deslizador de presupuesto en la moneda de la ciudad: "hasta 25" son euros en Madrid,
 * "hasta 100.000" pesos en Bogotá o "hasta 3.000" yenes en Tokio. Todas las monedas tienen el
 * mismo número de pasos, así que el paso elegido (lo que se guarda) se entiende igual en cualquier sitio.
 *
 * @property amounts máximos de cada paso; el primero es 0 (solo gratis). El paso siguiente al
 * último es "Sin límite".
 */
data class BudgetScale(val currency: String, val amounts: List<Double>) {
    /** Paso de "Sin límite", el último del deslizador. */
    val unlimitedStep: Int get() = amounts.size

    /** Presupuesto máximo de un paso, o `null` si es "Sin límite". */
    fun maxFor(step: Int?): Double? = step?.takeIf { it in amounts.indices }?.let { amounts[it] }

    companion object {
        /** Cualquier moneda: el paso base son unos 5 € en esa moneda, redondeados a una cifra cómoda. */
        fun forCurrency(currency: String): BudgetScale {
            val unit = niceAmount(5 * eurosIn(currency))
            return BudgetScale(currency, listOf(0, 1, 2, 3, 4, 5, 6, 8, 10, 15, 20).map { it * unit })
        }

        fun forCity(city: City): BudgetScale = forCurrency(city.currency)
    }
}

/**
 * Valor aproximado de 1 € en cada moneda (2026). Solo se usa para la escala del presupuesto y para
 * estimar precios, así que basta con el orden de magnitud. Si la moneda no está, se asume ≈ 1 €.
 */
fun eurosIn(currency: String): Double = EUR_RATES[currency] ?: 1.0

private val EUR_RATES = mapOf(
    // Europa
    "EUR" to 1.0, "GBP" to 0.85, "CHF" to 0.95, "SEK" to 11.0, "NOK" to 11.5, "DKK" to 7.5, "ISK" to 150.0,
    "PLN" to 4.3, "CZK" to 25.0, "HUF" to 400.0, "RON" to 5.0, "BGN" to 2.0, "RSD" to 117.0, "TRY" to 45.0,
    "UAH" to 47.0, "RUB" to 100.0,
    // América
    "USD" to 1.1, "CAD" to 1.5, "MXN" to 20.0, "GTQ" to 8.5, "CRC" to 560.0, "DOP" to 68.0, "COP" to 4_500.0,
    "PEN" to 4.1, "CLP" to 1_050.0, "ARS" to 1_300.0, "UYU" to 45.0, "PYG" to 8_500.0, "BOB" to 7.6, "BRL" to 6.2,
    "VES" to 50.0,
    // Asia, Oceanía y África
    "JPY" to 165.0, "KRW" to 1_500.0, "CNY" to 7.9, "HKD" to 8.6, "TWD" to 35.0, "SGD" to 1.45, "THB" to 38.0,
    "MYR" to 4.9, "IDR" to 18_000.0, "PHP" to 63.0, "VND" to 28_000.0, "INR" to 95.0, "AED" to 4.0, "SAR" to 4.1,
    "ILS" to 4.0, "QAR" to 4.0, "AUD" to 1.65, "NZD" to 1.8, "ZAR" to 20.0, "EGP" to 54.0, "MAD" to 10.8,
    "KES" to 140.0, "NGN" to 1_700.0,
)

/** Redondea a 1, 2 o 5 por una potencia de 10 (20.000, 500, 5…), la cifra "cómoda" más cercana. */
fun niceAmount(value: Double): Double {
    if (value <= 0) return 1.0
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(value)))
    return listOf(1.0, 2.0, 5.0, 10.0).map { it * magnitude }.minBy { Math.abs(it - value) }
}

/** Origen de un precio estimado: la IA (Gemini) o la regla local por categoría y recinto. */
enum class PriceEstimate { Local, Ai }

/** Precio típico en euros de la entrada más barata según la categoría, el recinto y el título. */
fun localPriceEstimateEur(event: Event): Double {
    val venue = event.venue.lowercase()
    val text = "${event.title} ${event.venue}".lowercase()
    fun has(vararg words: String) = words.any { it in text }
    fun venueHas(vararg words: String) = words.any { it in venue }
    return when {
        venueHas("estadi", "stadium", "metropolitano", "bernabéu", "bernabeu", "camp nou") -> 70.0
        venueHas("arena", "palau sant jordi", "wizink", "vistalegre", "palacio de los deportes", "ifema", "movistar") -> 55.0
        has("ópera", "opera", "liceu", "teatro real", "sinfónic", "sinfonic", "orquesta", "orchestra") -> 45.0
        has("tablao", "flamenco") -> 35.0
        venueHas("teatro", "teatre", "theatre", "auditori", "auditorio", "palau de la música") -> 32.0
        event.category == Category.Music && venueHas("sala", "club", "bar", "café", "cafe", "jazz", "razzmatazz", "apolo") -> 18.0
        event.category == Category.Music -> 28.0
        event.category == Category.Art || venueHas("museo", "museu", "museum", "galería", "galeria") -> 12.0
        event.category == Category.Markets -> 5.0
        event.category == Category.Fairs -> 15.0
        event.category == Category.Food -> 25.0
        else -> 22.0
    }
}

/**
 * Si el evento no es gratuito y no publica precio, le pone uno estimado con la regla local, en la
 * moneda de la ciudad. Los que ya tienen precio (real o estimado) no cambian.
 */
fun Event.withLocalPriceEstimate(currency: String): Event {
    if (isFree || priceMin != null) return this
    val amount = roundPrice(localPriceEstimateEur(this) * eurosIn(currency), currency)
    return copy(priceMin = amount, currency = currency, priceEstimate = PriceEstimate.Local)
}

/** Redondea a una cifra "de taquilla": euros enteros, miles de pesos, cientos de yenes… */
fun roundPrice(amount: Double, currency: String): Double {
    val unit = niceAmount(eurosIn(currency)).coerceAtLeast(1.0)
    return (Math.round(amount / unit) * unit).toDouble()
}

/**
 * ¿Entra el evento en el presupuesto? Sin máximo (`null`) entra todo; los gratuitos entran siempre.
 * Si no se conoce el precio, o está en otra moneda, no se puede asegurar y queda fuera.
 */
fun Event.fitsBudget(max: Double?, currency: String): Boolean {
    if (max == null || isFree) return true
    val amount = priceMin?.takeIf { this.currency == currency } ?: return false
    return amount <= max
}

/** Precio mínimo y moneda leídos de un texto como "15 €", "Desde 40.000 COP", "3.000 JPY" o "$ 20,50". */
fun parsePriceLabel(label: String?): Pair<Double, String>? {
    if (label.isNullOrBlank()) return null
    // Primero un código ISO explícito ("40.000 COP", "3000 JPY"); si no, el símbolo.
    val isoCode = Regex("""\b[A-Z]{3}\b""").findAll(label).map { it.value }
        .firstOrNull { code -> runCatching { java.util.Currency.getInstance(code) }.isSuccess }
    val currency = isoCode ?: when {
        "€" in label -> "EUR"
        "£" in label -> "GBP"
        "¥" in label -> "JPY"
        "$" in label -> "USD"
        else -> return null
    }
    // Formato español: punto para miles y coma para decimales.
    val number = Regex("""\d[\d.]*(,\d+)?""").find(label)?.value ?: return null
    val amount = number.replace(".", "").replace(",", ".").toDoubleOrNull() ?: return null
    return amount to currency
}
