package com.citypulse.domain.i18n

import java.util.Locale

/** Idiomas de la app. El español es el predeterminado, aunque el teléfono esté en otro idioma. */
enum class AppLanguage(val tag: String) {
    ES("es"),
    EN("en"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        val DEFAULT = ES

        /** El idioma de una etiqueta ("en-US" es inglés); cualquier otro, español. */
        fun of(tagOrLanguage: String?): AppLanguage =
            entries.firstOrNull { tagOrLanguage?.lowercase()?.startsWith(it.tag) == true } ?: DEFAULT
    }
}

/**
 * Idioma con el que se escriben los textos que no salen de los recursos (fechas, precios, etiquetas
 * de categoría). Lo fija la app al arrancar y al cambiar de idioma; las pruebas lo dejan en español.
 */
object AppLocale {
    @Volatile var language: AppLanguage = AppLanguage.DEFAULT

    val isEnglish: Boolean get() = language == AppLanguage.EN

    /** Locale para fechas y números: español de España (como el resto de la app) o inglés de EE. UU. */
    val current: Locale get() = if (isEnglish) Locale.US else Locale.forLanguageTag("es-ES")
}

/** El texto en el idioma actual. */
fun tr(es: String, en: String): String = if (AppLocale.isEnglish) en else es
