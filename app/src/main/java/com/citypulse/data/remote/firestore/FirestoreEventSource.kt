package com.citypulse.data.remote.firestore

import com.citypulse.domain.model.City
import com.citypulse.domain.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Catálogo propio de eventos (sobre todo gratuitos, mercados y ferias) guardado en Firestore. */
@Singleton
class FirestoreEventSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun events(city: City, from: LocalDate, days: Int = 7): List<Event> {
        val snapshot = firestore.collection(COLLECTION)
            .whereEqualTo("city", city.name)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { doc -> doc.data?.let { CatalogEntry.fromDocument(doc.id, it) } }
            .flatMap { it.occurrences(from, days) }
    }

    private companion object {
        const val COLLECTION = "events"
    }
}
