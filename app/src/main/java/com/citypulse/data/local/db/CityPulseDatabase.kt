package com.citypulse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedEventEntity::class], version = 1, exportSchema = false)
abstract class CityPulseDatabase : RoomDatabase() {
    abstract fun savedEventDao(): SavedEventDao
}
