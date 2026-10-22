package com.citypulse.data.repository

import com.citypulse.data.local.db.CachedEventDao
import com.citypulse.data.local.db.toCachedEntity
import com.citypulse.data.local.db.toEvent
import com.citypulse.data.remote.firestore.FirestoreEventSource
import com.citypulse.data.remote.ticketmaster.TicketmasterEventSource
import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.PriceEstimate
import com.citypulse.domain.model.RefreshState
import com.citypulse.domain.model.roundPrice
import com.citypulse.domain.model.withLocalPriceEstimate
import com.citypulse.domain.repository.EventAssistant
import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.time.Clock
import com.citypulse.domain.time.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 *
 * Ticketmaster ya no publica precios, así que a los eventos sin precio se les pone uno estimado:
 * primero la regla local (al instante) y después, en segundo plano, la estimación de la IA.
 */
@Singleton
class DefaultEventRepository @Inject constructor(
    private val ticketmaster: TicketmasterEventSource,
    private val catalog: FirestoreEventSource,
    private val cache: CachedEventDao,
    private val clock: Clock,
    private val assistant: EventAssistant,
) : EventRepository {

    private val refreshMutex = Mutex()
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _refreshState = MutableStateFlow(RefreshState.Idle)
    override val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    override fun observeEvents(city: City): Flow<List<Event>> = channelFlow {
        // Se marca "cargando" antes de emitir la caché: si está vacía, la pantalla muestra
        // esqueletos en vez de un "no hay eventos" momentáneo.
        val stale = isStale(city)
        if (stale) _refreshState.value = RefreshState.Loading
        launch { if (stale) refresh(city) }
        cache.observeByCity(city.key).collectLatest { rows -> send(rows.map { it.toEvent() }) }
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
        val previous = cache.getByCity(city.key).map { it.toEvent() }
        fun orPrevious(result: Result<List<Event>>, source: EventSource) =
            result.getOrElse { previous.filter { it.source == source } }

        val currency = city.currency
        // Las estimaciones de la IA de la descarga anterior se conservan: no se vuelve a preguntar.
        val previousAi = previous.filter { it.priceEstimate == PriceEstimate.Ai }.associateBy { it.id }
        val merged = mergeEvents(
            // El catálogo propio va primero: ante duplicados, su ficha es más completa.
            listOf(orPrevious(fromCatalog, EventSource.CityPulse), orPrevious(fromTicketmaster, EventSource.Ticketmaster)),
            now,
        ).map { event ->
            val ai = previousAi[event.id]
            when {
                event.isFree || event.priceMin != null -> event
                ai != null -> event.copy(priceMin = ai.priceMin, currency = ai.currency, priceEstimate = PriceEstimate.Ai)
                else -> event.withLocalPriceEstimate(currency)
            }
        }
        val fetchedAt = System.currentTimeMillis()
        cache.replaceCity(city.key, merged.map { it.toCachedEntity(city.key, fetchedAt) })
        _refreshState.value = RefreshState.Idle
        background.launch { refineWithAi(city, currency, merged.filter { it.priceEstimate == PriceEstimate.Local }) }
        Result.success(Unit)
    }

    /** Pide a la IA el precio de los eventos que solo tienen la estimación local y los actualiza. */
    private suspend fun refineWithAi(city: City, currency: String, events: List<Event>) {
        if (events.isEmpty()) return
        val prices = assistant.estimatePrices(events, "${city.name}, ${city.country}", currency).getOrNull().orEmpty()
        if (prices.isEmpty()) return
        refreshMutex.withLock {
            val updated = cache.getByCity(city.key).mapNotNull { row ->
                val price = prices[row.id] ?: return@mapNotNull null
                // Solo se reemplaza la estimación local; si mientras tanto llegó un precio real, se respeta.
                if (row.priceEstimate != PriceEstimate.Local.name) return@mapNotNull null
                row.copy(priceMin = roundPrice(price, currency), currency = currency, priceEstimate = PriceEstimate.Ai.name)
            }
            if (updated.isNotEmpty()) cache.insertAll(updated)
        }
    }

    override suspend fun getEvent(id: String): Event? = cache.get(id)?.toEvent()

    private suspend fun isStale(city: City): Boolean {
        val last = cache.lastFetch(city.key)
        return last == null || System.currentTimeMillis() - last > CACHE_TTL_MILLIS
    }
}
