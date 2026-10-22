package com.citypulse.data.remote.firestore

import com.citypulse.domain.geo.distanceMeters
import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Distancia máxima al centro del lugar para incluir un evento del catálogo (igual que Ticketmaster). */
private const val CATALOG_RADIUS_METERS = 15_000.0

/**
 * Catálogo propio de eventos (sobre todo gratuitos, mercados y ferias) guardado en Firestore.
 * Se filtra por distancia al lugar, no por el nombre de la ciudad: así sirve igual con una ciudad
 * de la lista, con la ubicación GPS ("San Juan de Pasto", un barrio, un pueblo cercano…) o con
 * cualquier lugar buscado.
 */
@Singleton
class FirestoreEventSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun events(city: City, from: LocalDate, days: Int = 7): List<Event> {
        val snapshot = firestore.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { doc -> doc.data?.let { CatalogEntry.fromDocument(doc.id, it) } }
            .flatMap { it.occurrences(from, days) }
            .filter { distanceMeters(it.location, city.center) <= CATALOG_RADIUS_METERS }
    }

    private companion object {
        const val COLLECTION = "events"
    }
}
