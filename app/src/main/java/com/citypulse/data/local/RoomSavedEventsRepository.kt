package com.citypulse.data.local

import com.citypulse.data.local.db.SavedEventDao
import com.citypulse.data.local.db.toEvent
import com.citypulse.data.local.db.toSavedEntity
import com.citypulse.domain.model.Event
import com.citypulse.domain.repository.SavedEventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSavedEventsRepository @Inject constructor(
    private val dao: SavedEventDao,
) : SavedEventsRepository {
    override fun observeSaved(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toEvent() } }
    override fun observeSavedIds(): Flow<Set<String>> = dao.observeIds().map { it.toSet() }
    override suspend fun save(event: Event) = dao.insert(event.toSavedEntity())
    override suspend fun remove(eventId: String) = dao.delete(eventId)
    override suspend fun isSaved(eventId: String): Boolean = dao.exists(eventId)
}
