package com.citypulse.domain.model

/** Cuenta con la que el usuario inició sesión. */
data class UserAccount(
    val uid: String,
    val name: String?,
    val email: String,
) {
    /** Nombre para mostrar: el elegido al registrarse o, si no hay, la parte del correo antes de la @. */
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    /** Inicial para el avatar. */
    val initial: String get() = displayName.firstOrNull()?.uppercase() ?: "?"
}
