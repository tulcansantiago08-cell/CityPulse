package com.citypulse.data.sync

import android.util.Log
import com.citypulse.data.local.db.SavedEventDao
import com.citypulse.data.local.db.SavedEventEntity
import com.citypulse.domain.repository.AuthRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Copia los guardados en `users/{uid}/saved/{eventId}` de Firestore mientras hay sesión iniciada.
 *
 * Al iniciar sesión se fusionan los dos lados: lo del teléfono se sube y lo de la cuenta se baja.
 * Después se escuchan los cambios de la cuenta (por ejemplo, desde otro móvil) y se aplican en Room.
 * Firestore guarda las escrituras sin conexión y las envía cuando vuelve la red.
 */
@Singleton
class SavedEventsCloudSync @Inject constructor(
    private val auth: AuthRepository,
    private val dao: SavedEventDao,
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var uid: String? = null

    fun start() {
        scope.launch {
            auth.currentUser.map { it?.uid }.distinctUntilChanged().collectLatest { id ->
                uid = id
                if (id != null) mirror(id)
            }
        }
    }

    fun upload(entity: SavedEventEntity) {
        val id = uid ?: return
        collection(id).document(entity.id).set(entity.toCloud())
            .addOnFailureListener { Log.w(TAG, "No se pudo subir ${entity.id}", it) }
    }

    fun delete(eventId: String) {
        val id = uid ?: return
        collection(id).document(eventId).delete()
            .addOnFailureListener { Log.w(TAG, "No se pudo borrar $eventId", it) }
    }

    private suspend fun mirror(uid: String) {
        var merged = false
        snapshots(uid).collect { snapshot ->
            // La primera respuesta del servidor decide qué falta en la cuenta; la caché puede estar incompleta.
            if (!merged && !snapshot.metadata.isFromCache) {
                merged = true
                val inCloud = snapshot.documents.map { it.id }.toSet()
                dao.getAll().filter { it.id !in inCloud }.forEach { upload(it) }
            }
            snapshot.documentChanges.forEach { change ->
                when (change.type) {
                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                        change.document.toSavedEntity()?.let { dao.insert(it) }
                    DocumentChange.Type.REMOVED -> dao.delete(change.document.id)
                }
            }
        }
    }

    private fun snapshots(uid: String): Flow<QuerySnapshot> = callbackFlow {
        val registration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) Log.w(TAG, "Sin acceso a los guardados de la cuenta", error)
            snapshot?.let { trySend(it) }
        }
        awaitClose { registration.remove() }
    }

    private fun collection(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("saved")

    private companion object {
        const val TAG = "CityPulseSync"
    }
}

internal fun SavedEventEntity.toCloud(): Map<String, Any?> = mapOf(
    "title" to title,
    "category" to category,
    "venue" to venue,
    "area" to area,
    "latitude" to latitude,
    "longitude" to longitude,
    "start" to start,
    "end" to end,
    "price" to price,
    "free" to free,
    "description" to description,
    "imageUrl" to imageUrl,
    "source" to source,
    "url" to url,
    "priceMin" to priceMin,
    "currency" to currency,
    "savedAt" to savedAt,
)

/** `null` si al documento le falta algún dato obligatorio. */
internal fun DocumentSnapshot.toSavedEntity(): SavedEventEntity? = runCatching {
    SavedEventEntity(
        id = id,
        title = getString("title")!!,
        category = getString("category")!!,
        venue = getString("venue").orEmpty(),
        area = getString("area").orEmpty(),
        latitude = getDouble("latitude")!!,
        longitude = getDouble("longitude")!!,
        start = getString("start")!!,
        end = getString("end")!!,
        price = getString("price"),
        free = getBoolean("free") ?: false,
        description = getString("description").orEmpty(),
        imageUrl = getString("imageUrl"),
        source = getString("source")!!,
        url = getString("url"),
        priceMin = getDouble("priceMin"),
        currency = getString("currency"),
        savedAt = getLong("savedAt") ?: System.currentTimeMillis(),
    )
}.getOrNull()
