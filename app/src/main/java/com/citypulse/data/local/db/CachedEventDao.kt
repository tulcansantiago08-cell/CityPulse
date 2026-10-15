package com.citypulse.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedEventDao {
    @Query("SELECT * FROM cached_events WHERE city = :city ORDER BY start")
    fun observeByCity(city: String): Flow<List<CachedEventEntity>>

    @Query("SELECT * FROM cached_events WHERE city = :city")
    suspend fun getByCity(city: String): List<CachedEventEntity>

    @Query("SELECT * FROM cached_events WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CachedEventEntity?

    @Query("SELECT MAX(fetchedAt) FROM cached_events WHERE city = :city")
    suspend fun lastFetch(city: String): Long?

    @Query("DELETE FROM cached_events WHERE city = :city")
    suspend fun deleteCity(city: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CachedEventEntity>)

    /** Sustituye de una vez los eventos de la ciudad (los observadores ven un solo cambio). */
    @Transaction
    suspend fun replaceCity(city: String, events: List<CachedEventEntity>) {
        deleteCity(city)
        insertAll(events)
    }
}
