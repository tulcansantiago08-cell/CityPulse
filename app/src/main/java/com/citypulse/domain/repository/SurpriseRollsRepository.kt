package com.citypulse.domain.repository

import com.citypulse.domain.model.SurpriseRolls
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SurpriseRollsRepository {
    /** Estado de las tiradas de [today] (se reinicia solo al cambiar de día). */
    fun observe(today: LocalDate): Flow<SurpriseRolls>

    suspend fun save(rolls: SurpriseRolls)
}
