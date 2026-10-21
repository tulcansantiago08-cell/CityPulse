package com.citypulse.domain.repository

import com.citypulse.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

/** Motivos por los que puede fallar el inicio de sesión o el registro. */
enum class AuthError { InvalidCredentials, EmailInUse, WeakPassword, InvalidEmail, TooManyRequests, Network, Unknown }

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)

/**
 * Cuenta del usuario (opcional: la app se usa igual sin iniciar sesión).
 * Las operaciones lanzan [AuthException] si fallan.
 */
interface AuthRepository {
    /** Cuenta actual, o `null` si no hay sesión iniciada. */
    val currentUser: Flow<UserAccount?>

    suspend fun signIn(email: String, password: String)
    suspend fun signUp(name: String, email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun signOut()
}
