package com.citypulse.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object HomeRoute
@Serializable data object NowRoute
@Serializable data object SurpriseRoute
@Serializable data object MapRoute
@Serializable data object SavedRoute
@Serializable data object CityRoute
@Serializable data object LocationDeniedRoute
@Serializable data object AiSearchRoute
@Serializable data class DetailRoute(val eventId: String)
