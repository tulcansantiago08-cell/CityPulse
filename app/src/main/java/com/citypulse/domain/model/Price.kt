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
