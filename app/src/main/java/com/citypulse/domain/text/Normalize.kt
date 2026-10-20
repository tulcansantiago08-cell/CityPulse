package com.citypulse.domain.text

import java.text.Normalizer

/** Minúsculas y sin tildes: "Bogotá" → "bogota". Para buscar sin importar acentos. */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
