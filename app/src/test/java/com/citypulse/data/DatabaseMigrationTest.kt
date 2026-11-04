package com.citypulse.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.citypulse.data.local.db.CityPulseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Crea la base de datos tal como la dejaba una versión vieja de la app y comprueba que, al abrirla
 * con la versión actual, las migraciones la llevan al esquema que espera Room sin perder guardados.
 *
 * Room valida tablas, columnas e índices al terminar de migrar: si una migración olvida algo,
 * el test falla con "Migration didn't properly handle…".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun deleteDatabase() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun `desde la version 1 llega a la ultima sin perder los guardados`() = runBlocking {
        createDatabase(version = 1, V1_SAVED_EVENTS) { db ->
            db.execSQL(savedRow(id = "gratis", price = null) + ", 1)")
            db.execSQL(savedRow(id = "pago", price = "'20 €'") + ", 2)")
        }

        withCurrentDatabase { room ->
            val saved = room.savedEventDao().getAll().associateBy { it.id }
            assertEquals(setOf("gratis", "pago"), saved.keys)
            // v2 deduce "gratis" de que no hubiera precio.
            assertTrue(saved.getValue("gratis").free)
            assertFalse(saved.getValue("pago").free)
            assertEquals("20 €", saved.getValue("pago").price)
            assertNull(saved.getValue("pago").priceMin)
            assertNull(saved.getValue("pago").priceEstimate)
            // v6: los guardados anteriores no tienen zona; los avisos usan la del teléfono.
            assertNull(saved.getValue("pago").zone)
            assertTrue(room.cachedEventDao().getByCity("Madrid|ES").isEmpty())
        }
    }

    @Test
    fun `desde la version 3 vacia la cache para volver a descargar con precios`() = runBlocking {
        createDatabase(version = 3, V3_SAVED_EVENTS, V3_CACHED_EVENTS, V3_CACHED_INDEX) { db ->
            db.execSQL(savedRow(id = "guardado", price = null) + ", 1, 1)")
            db.execSQL(
                "INSERT INTO cached_events VALUES ('viejo', 'Madrid|ES', 'Concierto', 'Music', 'Sala', 'Centro', " +
                    "40.4, -3.7, '2026-10-13T20:00', '2026-10-13T22:00', NULL, 0, '', NULL, 'Ticketmaster', NULL, 1)",
            )
        }

        withCurrentDatabase { room ->
            assertEquals(listOf("guardado"), room.savedEventDao().getAll().map { it.id })
            assertTrue(room.cachedEventDao().getByCity("Madrid|ES").isEmpty())
            assertNull(room.cachedEventDao().lastFetch("Madrid|ES"))
        }
    }

    /** Crea el archivo de la base con las tablas de [version], como lo habría hecho esa versión de la app. */
    private fun createDatabase(version: Int, vararg statements: String, fill: (android.database.sqlite.SQLiteDatabase) -> Unit) {
        context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null).use { db ->
            statements.forEach(db::execSQL)
            fill(db)
            db.version = version
        }
    }

    /** Abre la base con la versión actual de la app (esto ejecuta las migraciones y la validación). */
    private suspend fun withCurrentDatabase(block: suspend (CityPulseDatabase) -> Unit) {
        val room = Room.databaseBuilder(context, CityPulseDatabase::class.java, DB_NAME)
            .addMigrations(*CityPulseDatabase.ALL_MIGRATIONS)
            .build()
        try {
            block(room)
        } finally {
            room.close()
        }
    }

    private fun savedRow(id: String, price: String?) =
        "INSERT INTO saved_events VALUES ('$id', 'Evento $id', 'Market', 'Plaza Mayor', 'Centro', 40.4, -3.7, " +
            "'2026-10-13T10:00', '2026-10-13T12:00', ${price ?: "NULL"}, 'Descripción', NULL, 'CityPulse', NULL"

    private companion object {
        const val DB_NAME = "migration-test.db"

        /** Tabla tal como la creaba Room en la v1 (commit 1ace470). La última columna es savedAt. */
        val V1_SAVED_EVENTS = """
            CREATE TABLE IF NOT EXISTS saved_events (
                id TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, venue TEXT NOT NULL,
                area TEXT NOT NULL, latitude REAL NOT NULL, longitude REAL NOT NULL, start TEXT NOT NULL,
                `end` TEXT NOT NULL, price TEXT, description TEXT NOT NULL, imageUrl TEXT,
                source TEXT NOT NULL, url TEXT, savedAt INTEGER NOT NULL, PRIMARY KEY(id)
            )
        """.trimIndent()

        /** v3 = v1 + `free` al final (ALTER TABLE de la v2), igual que en el teléfono de un usuario. */
        val V3_SAVED_EVENTS = V1_SAVED_EVENTS.replace(
            "savedAt INTEGER NOT NULL, PRIMARY KEY(id)",
            "savedAt INTEGER NOT NULL, free INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id)",
        )

        val V3_CACHED_EVENTS = """
            CREATE TABLE IF NOT EXISTS cached_events (
                id TEXT NOT NULL PRIMARY KEY, city TEXT NOT NULL, title TEXT NOT NULL,
                category TEXT NOT NULL, venue TEXT NOT NULL, area TEXT NOT NULL,
                latitude REAL NOT NULL, longitude REAL NOT NULL, start TEXT NOT NULL,
                `end` TEXT NOT NULL, price TEXT, free INTEGER NOT NULL, description TEXT NOT NULL,
                imageUrl TEXT, source TEXT NOT NULL, url TEXT, fetchedAt INTEGER NOT NULL
            )
        """.trimIndent()

        const val V3_CACHED_INDEX = "CREATE INDEX IF NOT EXISTS index_cached_events_city ON cached_events (city)"
    }
}
