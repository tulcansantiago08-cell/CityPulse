package com.citypulse.data

import com.citypulse.data.ai.SearchIntentDto
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.TimeSlot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIntentDtoTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun `convierte la respuesta de la ia e ignora valores desconocidos`() {
        val text = """{"categories":["Music","Dance"],"freeOnly":true,"maxPrice":0,"timeSlot":"Night",
            "todayOnly":true,"liveNow":false,"maxWalkMinutes":15,"keywords":["jazz",""]}"""
        val intent = json.decodeFromString<SearchIntentDto>(text).toDomain()
        assertEquals(setOf(Category.Music), intent.categories)
        assertTrue(intent.freeOnly)
        assertNull(intent.maxPrice) // 0 no es un tope útil
        assertEquals(TimeSlot.Night, intent.timeSlot)
        assertEquals(15, intent.maxWalkMinutes)
        assertEquals(listOf("jazz"), intent.keywords)
    }
}
