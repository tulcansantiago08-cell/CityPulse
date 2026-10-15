package com.citypulse.data.repository

import com.citypulse.data.local.db.CachedEventDao
import com.citypulse.data.local.db.toCachedEntity
import com.citypulse.data.local.db.toEvent
import com.citypulse.data.remote.firestore.FirestoreEventSource
import com.citypulse.data.remote.ticketmaster.TicketmasterEventSource
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.time.Clock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Pasado este tiempo, al abrir la ciudad se vuelve a pedir a las fuentes. */
private const val CACHE_TTL_MILLIS = 30 * 60 * 1000L

/**
 * Repositorio "cache then network": emite enseguida lo guardado en Room y, si está viejo,
 * descarga de Ticketmaster y del catálogo de Firestore en paralelo y actualiza la caché.
 */
@Singleton
class DefaultEventRepository @Inject constructor(
    private val ticketmaster: TicketmasterEventSource,
    private val catalog: FirestoreEventSource,
    private val cache: CachedEventDao,
    private val clock: Clock,
) : EventRepository {

    private val refreshMutex = Mutex()

    override fun observeEvents(city: City): Flow<List<Event>> = channelFlow {
        launch { refreshIfStale(city) }
        cache.observeByCity(city.name).collectLatest { rows -> send(rows.map { it.toEvent() }) }
    }

    override suspend fun refresh(city: City): Result<Unit> = refreshMutex.withLock {
        val now = clock.now()
        val (fromTicketmaster, fromCatalog) = coroutineScope {
            val tm = async { runCatching { ticketmaster.events(city.center, now, now.plusDays(7)) } }
            val fs = async { runCatching { catalog.events(city, now.toLocalDate()) } }
            tm.await() to fs.await()
        }
        if (fromTicketmaster.isFailure && fromCatalog.isFailure) {
            return Result.failure(fromCatalog.exceptionOrNull() ?: fromTicketmaster.exceptionOrNull()!!)
        }
        // Si una fuente falla, se conservan sus eventos de la caché anterior.
        val previous = cache.getByCity(city.name).map { it.toEvent() }
        fun orPrevious(result: Result<List<Event>>, source: EventSource) =
            result.getOrElse { previous.filter { it.source == source } }

        val merged = mergeEvents(
            // El catálogo propio va primero: ante duplicados, su ficha es más completa.
            listOf(orPrevious(fromCatalog, EventSource.CityPulse), orPrevious(fromTicketmaster, EventSource.Ticketmaster)),
            now,
        )
        val fetchedAt = System.currentTimeMillis()
        cache.replaceCity(city.name, merged.map { it.toCachedEntity(city.name, fetchedAt) })
        Result.success(Unit)
    }

    override suspend fun getEvent(id: String): Event? = cache.get(id)?.toEvent()

    private suspend fun refreshIfStale(city: City) {
        val last = cache.lastFetch(city.name)
        if (last == null || System.currentTimeMillis() - last > CACHE_TTL_MILLIS) refresh(city)
    }
}
