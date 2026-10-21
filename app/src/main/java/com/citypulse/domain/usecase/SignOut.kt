package com.citypulse.domain.usecase

import com.citypulse.domain.repository.AuthRepository
import com.citypulse.domain.repository.SavedEventsRepository
import javax.inject.Inject

/**
 * Cierra la sesión y quita del teléfono los guardados de esa cuenta,
 * para que no se mezclen con los de la siguiente persona que inicie sesión.
 */
class SignOut @Inject constructor(
    private val auth: AuthRepository,
    private val saved: SavedEventsRepository,
) {
    suspend operator fun invoke() {
        auth.signOut()
        saved.clearLocal()
    }
}
