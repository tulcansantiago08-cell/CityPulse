package com.citypulse.data

import com.citypulse.data.fake.SampleData
import com.citypulse.data.local.db.CachedEventDao
import com.citypulse.data.local.db.CachedEventEntity
import com.citypulse.data.local.db.toCachedEntity
import com.citypulse.data.remote.firestore.FirestoreEventSource
import com.citypulse.data.remote.ticketmaster.TicketmasterEventSource
import com.citypulse.data.repository.DefaultEventRepository
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.EventSource
import com.citypulse.domain.model.NearbyEvent
import com.citypulse.domain.model.PriceEstimate
import com.citypulse.domain.model.RefreshState
import com.citypulse.domain.model.SearchIntent
import com.citypulse.domain.repository.EventAssistant
import com.citypulse.domain.testEvent
import com.citypulse.ui.FixedClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultEventRepositoryTest {

    private val now = LocalDateTime.of(2026, 10, 13, 18, 0)
    private val city = SampleData.Madrid

    private val catalogEvent = testEvent("cp", start = now.plusHours(1)).copy(title = "Mercado de Motores")
    private val paidConcert = testEvent("tm", start = now.plusHours(3), price = "Entradas")
        .copy(title = "Concierto", source = EventSource.Ticketmaster)
    private val duplicate = testEvent("tm-dup", start = now.plusHours(1))
        .copy(title = "MERCADO DE MOTORES", source = EventSource.Ticketmaster)

    private val dao = FakeCachedEventDao()
    private val assistant = FakePriceAssistant()
    private val ticketmaster = mockk<TicketmasterEventSource>()
    private val catalog = mockk<FirestoreEventSource>()

    private fun repository() = DefaultEventRepository(ticketmaster, catalog, dao, FixedClock(now, city.zone), assistant)

    private fun ticketmasterReturns(vararg events: Event) =
        coEvery { ticketmaster.events(any(), any(), any(), any(), any()) } returns events.toList()

    private fun catalogReturns(vararg events: Event) =
        coEvery { catalog.events(any(), any(), any()) } returns events.toList()

    @Test
    fun `descarga las dos fuentes, quita duplicados y guarda en la cache de la ciudad`() = runTest {
        ticketmasterReturns(paidConcert, duplicate)
        catalogReturns(catalogEvent)
        val repo = repository()

        assertTrue(repo.refresh(city).isSuccess)

        val cached = dao.getByCity(city.key)
        // Ante duplicados gana el catálogo propio, que tiene la ficha más completa.
        assertEquals(setOf("cp", "tm"), cached.map { it.id }.toSet())
        assertEquals(RefreshState.Idle, repo.refreshState.value)
    }

    @Test
    fun `si una fuente falla conserva sus eventos de la descarga anterior`() = runTest {
        val previousConcert = paidConcert.copy(id = "tm-old", title = "Concierto anterior")
        dao.insertAll(listOf(previousConcert.toCachedEntity(city.key, fetchedAt = 0)))
        coEvery { ticketmaster.events(any(), any(), any(), any(), any()) } throws IOException("sin red")
        catalogReturns(catalogEvent)

        assertTrue(repository().refresh(city).isSuccess)

        assertEquals(setOf("cp", "tm-old"), dao.getByCity(city.key).map { it.id }.toSet())
    }

    @Test
    fun `si fallan las dos fuentes avisa del error y no toca la cache`() = runTest {
        dao.insertAll(listOf(catalogEvent.toCachedEntity(city.key, fetchedAt = 0)))
        coEvery { ticketmaster.events(any(), any(), any(), any(), any()) } throws IOException("sin red")
        coEvery { catalog.events(any(), any(), any()) } throws IOException("sin red")
        val repo = repository()

        assertTrue(repo.refresh(city).isFailure)

        assertEquals(RefreshState.Error, repo.refreshState.value)
        assertEquals(listOf("cp"), dao.getByCity(city.key).map { it.id })
    }

    @Test
    fun `con la cache reciente no vuelve a descargar`() = runTest {
        dao.insertAll(listOf(catalogEvent.toCachedEntity(city.key, fetchedAt = System.currentTimeMillis())))
        ticketmasterReturns()
        catalogReturns()

        val events = repository().observeEvents(city).first { it.isNotEmpty() }

        assertEquals(listOf("cp"), events.map { it.id })
        coVerify(exactly = 0) { catalog.events(any(), any(), any()) }
        coVerify(exactly = 0) { ticketmaster.events(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `con la cache de hace mas de 30 minutos descarga de nuevo`() = runTest {
        val old = System.currentTimeMillis() - 31 * 60 * 1000L
        dao.insertAll(listOf(catalogEvent.toCachedEntity(city.key, fetchedAt = old)))
        ticketmasterReturns(paidConcert)
        catalogReturns(catalogEvent)

        val events = repository().observeEvents(city).first { list -> list.any { it.id == "tm" } }

        assertEquals(setOf("cp", "tm"), events.map { it.id }.toSet())
        coVerify(exactly = 1) { catalog.events(any(), any(), any()) }
    }

    @Test
    fun `sin precio publicado pone la estimacion local y despues la de la IA`() = runTest {
        assistant.prices = mapOf("tm" to 42.0)
        ticketmasterReturns(paidConcert)
        catalogReturns(catalogEvent)

        repository().refresh(city)

        // Al instante: estimación local, para no dejar la tarjeta sin precio.
        val local = dao.get("tm")!!
        assertTrue(local.priceEstimate == PriceEstimate.Local.name || local.priceEstimate == PriceEstimate.Ai.name)
        // En segundo plano llega la estimación de la IA y la reemplaza.
        val refined = waitFor { dao.get("tm")?.takeIf { it.priceEstimate == PriceEstimate.Ai.name } }
        assertEquals(42.0, refined.priceMin!!, 0.0)
        assertEquals("EUR", refined.currency)
        // A los eventos gratis no se les pregunta el precio.
        assertFalse("cp" in assistant.asked)
    }

    @Test
    fun `no vuelve a preguntar a la IA un precio que ya estimo`() = runTest {
        val alreadyEstimated = paidConcert.copy(priceMin = 30.0, currency = "EUR", priceEstimate = PriceEstimate.Ai)
        dao.insertAll(listOf(alreadyEstimated.toCachedEntity(city.key, fetchedAt = 0)))
        ticketmasterReturns(paidConcert)
        catalogReturns()

        repository().refresh(city)

        val cached = dao.get("tm")!!
        assertEquals(PriceEstimate.Ai.name, cached.priceEstimate)
        assertEquals(30.0, cached.priceMin!!, 0.0)
        assertTrue(assistant.asked.isEmpty())
    }

    /** Espera (con tiempo real) a que el trabajo en segundo plano del repositorio deje un resultado. */
    private suspend fun <T : Any> waitFor(read: suspend () -> T?): T = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            var value = read()
            while (value == null) {
                delay(20)
                value = read()
            }
            value
        }
    }
}

/** Caché en memoria con el mismo comportamiento que [CachedEventDao] en Room. */
private class FakeCachedEventDao : CachedEventDao {
    private val rows = MutableStateFlow<Map<String, CachedEventEntity>>(emptyMap())

    override fun observeByCity(city: String): Flow<List<CachedEventEntity>> =
        rows.map { all -> all.values.filter { it.city == city }.sortedBy { it.start } }

    override suspend fun getByCity(city: String): List<CachedEventEntity> = rows.value.values.filter { it.city == city }
    override suspend fun get(id: String): CachedEventEntity? = rows.value[id]
    override suspend fun lastFetch(city: String): Long? = getByCity(city).maxOfOrNull { it.fetchedAt }

    override suspend fun deleteCity(city: String) {
        rows.update { all -> all.filterValues { it.city != city } }
    }

    override suspend fun insertAll(events: List<CachedEventEntity>) {
        rows.update { all -> all + events.associateBy { it.id } }
    }
}

/** Solo responde a la estimación de precios; anota qué eventos se le preguntaron. */
private class FakePriceAssistant : EventAssistant {
    @Volatile var prices: Map<String, Double> = emptyMap()
    val asked = CopyOnWriteArrayList<String>()

    override suspend fun estimatePrices(events: List<Event>, cityName: String, currency: String): Result<Map<String, Double>> {
        asked += events.map { it.id }
        return Result.success(prices)
    }

    override suspend fun interpret(query: String, cityName: String, currency: String, now: LocalDateTime): Result<SearchIntent> =
        Result.failure(UnsupportedOperationException())

    override suspend fun summarize(event: Event): Result<String> = Result.failure(UnsupportedOperationException())

    override suspend fun explainSurprise(event: NearbyEvent, now: LocalDateTime): Result<String> =
        Result.failure(UnsupportedOperationException())
}
