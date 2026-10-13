package com.citypulse.data.fake

import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Repositorio en memoria con los eventos del prototipo. Solo tiene datos de Madrid. */
@Singleton
class FakeEventRepository @Inject constructor() : EventRepository {

    private val events = MutableStateFlow(SampleData.events(LocalDate.now()))

    override fun observeEvents(city: City): Flow<List<Event>> =
        events.map { all -> if (city.name == SampleData.Madrid.name) all else emptyList() }

    override suspend fun refresh(city: City): Result<Unit> {
        events.value = SampleData.events(LocalDate.now())
        return Result.success(Unit)
    }

    override suspend fun getEvent(id: String): Event? = events.value.firstOrNull { it.id == id }
}
