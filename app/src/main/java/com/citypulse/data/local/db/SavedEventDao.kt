package com.citypulse.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEventDao {
    @Query("SELECT * FROM saved_events ORDER BY start")
    fun observeAll(): Flow<List<SavedEventEntity>>

    @Query("SELECT id FROM saved_events")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_events WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SavedEventEntity)

    @Query("DELETE FROM saved_events WHERE id = :id")
    suspend fun delete(id: String)
}
