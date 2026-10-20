package com.citypulse.data.repository

import com.citypulse.data.local.db.CachedEventDao
import com.citypulse.data.local.db.toCachedEntity
import com.citypulse.data.local.db.toEvent
import com.citypulse.data.remote.firestore.FirestoreEventSource
import com.citypulse.data.remote.ticketmaster.TicketmasterEventSource
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.RefreshState
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.now
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _refreshState = MutableStateFlow(RefreshState.Idle)
    override val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    override fun observeEvents(city: City): Flow<List<Event>> = channelFlow {
        // Se marca "cargando" antes de emitir la caché: si está vacía, la pantalla muestra
        // esqueletos en vez de un "no hay eventos" momentáneo.
        val stale = isStale(city)
        if (stale) _refreshState.value = RefreshState.Loading
        launch { if (stale) refresh(city) }
        cache.observeByCity(city.name).collectLatest { rows -> send(rows.map { it.toEvent() }) }
    }

    override suspend fun refresh(city: City): Result<Unit> = refreshMutex.withLock {
        _refreshState.value = RefreshState.Loading
        val now = clock.now(city.zone)
        val (fromTicketmaster, fromCatalog) = coroutineScope {
            val tm = async { runCatching { ticketmaster.events(city.center, now, now.plusDays(7), city.zone) } }
            val fs = async { runCatching { catalog.events(city, now.toLocalDate()) } }
            tm.await() to fs.await()
        }
        if (fromTicketmaster.isFailure && fromCatalog.isFailure) {
            _refreshState.value = RefreshState.Error
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
        _refreshState.value = RefreshState.Idle
        Result.success(Unit)
    }

    override suspend fun getEvent(id: String): Event? = cache.get(id)?.toEvent()

    private suspend fun isStale(city: City): Boolean {
        val last = cache.lastFetch(city.name)
        return last == null || System.currentTimeMillis() - last > CACHE_TTL_MILLIS
    }
}
