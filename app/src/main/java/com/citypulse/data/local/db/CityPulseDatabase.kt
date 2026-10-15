package com.citypulse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedEventEntity::class, CachedEventEntity::class], version = 3, exportSchema = false)
abstract class CityPulseDatabase : RoomDatabase() {
    abstract fun savedEventDao(): SavedEventDao
    abstract fun cachedEventDao(): CachedEventDao

    companion object {
        /** v2: columna `free`. En v1 un evento era gratis cuando no tenía precio. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_events ADD COLUMN free INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE saved_events SET free = 1 WHERE price IS NULL")
            }
        }

        /** v3: tabla de caché de eventos por ciudad. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_events (
                        id TEXT NOT NULL PRIMARY KEY, city TEXT NOT NULL, title TEXT NOT NULL,
                        category TEXT NOT NULL, venue TEXT NOT NULL, area TEXT NOT NULL,
                        latitude REAL NOT NULL, longitude REAL NOT NULL, start TEXT NOT NULL,
                        `end` TEXT NOT NULL, price TEXT, free INTEGER NOT NULL, description TEXT NOT NULL,
                        imageUrl TEXT, source TEXT NOT NULL, url TEXT, fetchedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cached_events_city ON cached_events (city)")
            }
        }
    }
}
