package com.citypulse.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    /** `true` mientras haya una red con acceso a internet. */
    val isOnline: Flow<Boolean>
}
