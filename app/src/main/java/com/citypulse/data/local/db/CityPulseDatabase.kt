package com.citypulse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedEventEntity::class], version = 2, exportSchema = false)
abstract class CityPulseDatabase : RoomDatabase() {
    abstract fun savedEventDao(): SavedEventDao

    companion object {
        /** v2: columna `free`. En v1 un evento era gratis cuando no tenía precio. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_events ADD COLUMN free INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE saved_events SET free = 1 WHERE price IS NULL")
            }
        }
    }
}
