package com.citypulse.domain.repository

import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    /** Eventos conocidos de la ciudad (de la caché local mientras se actualizan). */
    fun observeEvents(city: City): Flow<List<Event>>

    /** Vuelve a pedir los eventos a las fuentes remotas. */
    suspend fun refresh(city: City): Result<Unit>

    suspend fun getEvent(id: String): Event?
}
