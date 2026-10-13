package com.citypulse.domain.usecase

import com.citypulse.domain.repository.EventRepository
import com.citypulse.domain.repository.SavedEventsRepository
import javax.inject.Inject

/** Guarda el evento si no lo estaba, o lo quita de guardados si ya lo estaba. */
class ToggleSavedEvent @Inject constructor(
    private val events: EventRepository,
    private val saved: SavedEventsRepository,
) {
    suspend operator fun invoke(eventId: String) {
        if (saved.isSaved(eventId)) {
            saved.remove(eventId)
        } else {
            events.getEvent(eventId)?.let { saved.save(it) }
        }
    }
}
