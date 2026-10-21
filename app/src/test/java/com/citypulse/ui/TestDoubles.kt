package com.citypulse.ui

import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.PriceRange
import com.citypulse.domain.model.SurpriseRolls
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.repository.LocationRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.repository.SurpriseRollsRepository
import com.citypulse.domain.repository.UserPreferencesRepository
import com.citypulse.domain.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Usa un dispatcher de test como Dispatchers.Main (viewModelScope lo necesita). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Hora fija, expresada en la hora local de [zone] (Madrid, como los datos de ejemplo). */
class FixedClock(time: LocalDateTime, zone: ZoneId = SampleData.Madrid.zone) : Clock {
    private val instant: Instant = time.atZone(zone).toInstant()
    override fun instant(): Instant = instant
    override fun ticks(periodMillis: Long): Flow<Instant> = flowOf(instant)
}

class FakePreferences(initial: UserPreferences = UserPreferences(city = SampleData.Madrid, onboardingDone = true)) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state
    override suspend fun setCity(city: City) = state.update { it.copy(city = city) }
    override suspend fun setFreeOnly(freeOnly: Boolean) = state.update { it.copy(freeOnly = freeOnly) }
    override suspend fun setPriceRange(range: PriceRange) = state.update { it.copy(priceRange = range) }
    override suspend fun setRadiusMinutes(minutes: Int) = state.update { it.copy(radiusMinutes = minutes) }
    override suspend fun setOnboardingDone() = state.update { it.copy(onboardingDone = true) }
}

class FakeLocation(private val point: GeoPoint? = SampleData.PuertaDelSol) : LocationRepository {
    override suspend fun currentLocation(): GeoPoint? = point
    override suspend fun cityAt(point: GeoPoint): City = SampleData.Madrid
}

class FakeSaved : SavedEventsRepository {
    private val saved = MutableStateFlow<Map<String, Event>>(emptyMap())
    override fun observeSaved(): Flow<List<Event>> = saved.map { it.values.toList() }
    override fun observeSavedIds(): Flow<Set<String>> = saved.map { it.keys }
    override suspend fun save(event: Event) = saved.update { it + (event.id to event) }
    override suspend fun remove(eventId: String) = saved.update { it - eventId }
    override suspend fun isSaved(eventId: String) = eventId in saved.value
    override suspend fun clearLocal() = saved.update { emptyMap() }
}

class FakeRolls(today: LocalDate) : SurpriseRollsRepository {
    val state = MutableStateFlow(SurpriseRolls(today))
    override fun observe(today: LocalDate): Flow<SurpriseRolls> = state.map { it.on(today) }
    override suspend fun save(rolls: SurpriseRolls) { state.value = rolls }
}

/** Asistente de IA falso: respuestas fijas o fallo, según el test. */
class FakeAssistant(private val available: Boolean = true) : com.citypulse.domain.repository.EventAssistant {
    var lastQuery: String? = null
    override suspend fun interpret(query: String, cityName: String, currency: String, now: LocalDateTime) =
        if (available) Result.success(com.citypulse.domain.model.SearchIntent(freeOnly = true).also { lastQuery = query })
        else Result.failure(IllegalStateException("sin IA"))
    override suspend fun summarize(event: Event) =
        if (available) Result.success("Resumen de ${event.title}") else Result.failure(IllegalStateException("sin IA"))
    override suspend fun explainSurprise(event: com.citypulse.domain.model.NearbyEvent, now: LocalDateTime) =
        if (available) Result.success("Ve a ${event.event.title}") else Result.failure(IllegalStateException("sin IA"))
}

class FakeAuth(var failWith: com.citypulse.domain.repository.AuthError? = null) : com.citypulse.domain.repository.AuthRepository {
    val user = MutableStateFlow<com.citypulse.domain.model.UserAccount?>(null)
    val resetRequests = mutableListOf<String>()
    override val currentUser: Flow<com.citypulse.domain.model.UserAccount?> = user
    private fun check() = failWith?.let { throw com.citypulse.domain.repository.AuthException(it) }
    override suspend fun signIn(email: String, password: String) {
        check(); user.value = com.citypulse.domain.model.UserAccount("uid", null, email)
    }
    override suspend fun signUp(name: String, email: String, password: String) {
        check(); user.value = com.citypulse.domain.model.UserAccount("uid", name, email)
    }
    override suspend fun sendPasswordReset(email: String) { check(); resetRequests += email }
    override suspend fun signOut() { user.value = null }
}
