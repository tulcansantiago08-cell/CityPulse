package com.citypulse.domain.repository

import com.citypulse.domain.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Eventos guardados por el usuario; se conservan en el dispositivo para verlos sin conexión.
 * Con sesión iniciada también se copian en la cuenta.
 */
interface SavedEventsRepository {
    fun observeSaved(): Flow<List<Event>>
    fun observeSavedIds(): Flow<Set<String>>
    suspend fun save(event: Event)
    suspend fun remove(eventId: String)
    suspend fun isSaved(eventId: String): Boolean

    /** Borra la copia del teléfono (al cerrar sesión); la de la cuenta se conserva. */
    suspend fun clearLocal()
}
