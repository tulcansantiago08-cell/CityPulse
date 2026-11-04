package com.citypulse.domain.usecase

import com.citypulse.domain.repository.AuthError
import com.citypulse.domain.repository.AuthException
import com.citypulse.domain.repository.AuthRepository
import com.citypulse.domain.repository.SavedEventsRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Elimina la cuenta y todo lo que guardó: primero confirma la contraseña, después borra los
 * guardados de la nube (las reglas de Firestore solo lo permiten con la sesión todavía abierta),
 * luego la cuenta y por último la copia del teléfono.
 *
 * Lanza [AuthException] si algo falla; en ese caso la cuenta sigue existiendo.
 */
class DeleteAccount @Inject constructor(
    private val auth: AuthRepository,
    private val saved: SavedEventsRepository,
) {
    suspend operator fun invoke(password: String) {
        auth.reauthenticate(password)
        try {
            saved.clearCloud()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AuthException(AuthError.Network, e)
        }
        auth.deleteAccount()
        saved.clearLocal()
    }
}
