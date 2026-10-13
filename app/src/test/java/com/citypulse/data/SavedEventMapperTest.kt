package com.citypulse.data

import com.citypulse.data.fake.SampleData
import com.citypulse.data.local.db.toEvent
import com.citypulse.data.local.db.toSavedEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SavedEventMapperTest {
    @Test
    fun `un evento guardado se recupera identico`() {
        SampleData.events(LocalDate.of(2026, 10, 13)).forEach { event ->
            assertEquals(event, event.toSavedEntity(savedAt = 0).toEvent())
        }
    }
}
