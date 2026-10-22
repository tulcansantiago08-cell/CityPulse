package com.citypulse.domain.model

/**
 * Escala del deslizador de presupuesto en la moneda de la ciudad: "hasta 25" son euros en Madrid
 * y "hasta 50.000" pesos en Bogotá. Todas las monedas tienen el mismo número de pasos, así que el
 * paso elegido (lo que se guarda) se entiende igual al cambiar de ciudad.
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
        /** Moneda habitual del país de la ciudad (por nombre, como lo guarda [City]). */
        fun currencyFor(country: String): String = when (country.lowercase()) {
            "españa", "portugal", "francia", "alemania", "italia" -> "EUR"
            "colombia" -> "COP"
            "méxico", "mexico" -> "MXN"
            "argentina" -> "ARS"
            else -> "USD"
        }

        fun forCurrency(currency: String): BudgetScale = when (currency) {
            "COP" -> BudgetScale(currency, steps(10_000.0))
            "MXN" -> BudgetScale(currency, steps(100.0))
            "ARS" -> BudgetScale(currency, steps(5_000.0))
            else -> BudgetScale(currency, steps(5.0)) // EUR, USD y otras de valor parecido
        }

        fun forCity(city: City): BudgetScale = forCurrency(currencyFor(city.country))

        /** 0, 1, 2, 3, 4, 5, 6, 8, 10, 15 y 20 veces la unidad: pasos finos en lo barato. */
        private fun steps(unit: Double) = listOf(0, 1, 2, 3, 4, 5, 6, 8, 10, 15, 20).map { it * unit }
    }
}

/** Origen de un precio estimado: la IA (Gemini) o la regla local por categoría y recinto. */
enum class PriceEstimate { Local, Ai }

/**
 * Equivalencia aproximada de 1 € en cada moneda, coherente con los pasos de [BudgetScale]
 * (5 € ≈ 10.000 COP ≈ 100 MXN ≈ 5.000 ARS). Solo se usa para precios estimados.
 */
fun eurosIn(currency: String): Double = when (currency) {
    "COP" -> 2_000.0
    "MXN" -> 20.0
    "ARS" -> 1_000.0
    else -> 1.0
}

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

/** Redondea a una cifra "de taquilla": euros enteros, miles de pesos colombianos, etc. */
fun roundPrice(amount: Double, currency: String): Double {
    val unit = when (currency) {
        "COP", "ARS" -> 1_000.0
        "MXN" -> 10.0
        else -> 1.0
    }
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
