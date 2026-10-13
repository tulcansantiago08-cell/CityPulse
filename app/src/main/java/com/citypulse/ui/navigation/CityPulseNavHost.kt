package com.citypulse.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.citypulse.core.designsystem.component.FloatingNavBar
import com.citypulse.core.designsystem.component.NavTab
import com.citypulse.ui.city.CityRoute
import com.citypulse.ui.detail.DetailRoute
import com.citypulse.ui.home.HomeRoute
import com.citypulse.ui.now.NowRoute
import com.citypulse.ui.saved.SavedRoute
import com.citypulse.ui.surprise.SurpriseRoute
import com.citypulse.ui.onboarding.OnboardingScreen

/** Raíz de la navegación: NavHost y, encima, la barra flotante solo en las pestañas. */
@Composable
fun CityPulseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.toNavTab()

    Box(modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = OnboardingRoute) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    // El permiso real se pide en la tarea 3.2; por ahora se entra directamente.
                    onEnableLocation = { navController.navigateToHomeClearingBackStack() },
                    onChooseCity = { navController.navigate(CityRoute) },
                )
            }
            composable<HomeRoute> {
                HomeRoute(
                    onOpenEvent = { navController.navigate(DetailRoute(it)) },
                    onChangeCity = { navController.navigate(CityRoute) },
                )
            }
            composable<NowRoute> { NowRoute(onOpenEvent = { navController.navigate(DetailRoute(it)) }) }
            composable<SurpriseRoute> {
                SurpriseRoute(
                    onOpenEvent = { navController.navigate(DetailRoute(it)) },
                    onSeeNearby = { navController.navigateToTab(NavTab.Now) },
                    onGoHome = { navController.navigateToTab(NavTab.Home) },
                )
            }
            composable<MapRoute> { Placeholder("Mapa") }
            composable<SavedRoute> { SavedRoute(onOpenEvent = { navController.navigate(DetailRoute(it)) }) }
            composable<CityRoute> {
                CityRoute(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.navigateToHomeClearingBackStack() },
                )
            }
            composable<LocationDeniedRoute> { Placeholder("Sin permiso") }
            composable<DetailRoute> { DetailRoute(onBack = { navController.popBackStack() }) }
        }

        if (currentTab != null) {
            FloatingNavBar(
                selected = currentTab,
                onSelect = { navController.navigateToTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }
}

private fun NavDestination.toNavTab(): NavTab? = when {
    hasRoute<HomeRoute>() -> NavTab.Home
    hasRoute<NowRoute>() -> NavTab.Now
    hasRoute<SurpriseRoute>() -> NavTab.Surprise
    hasRoute<MapRoute>() -> NavTab.Map
    hasRoute<SavedRoute>() -> NavTab.Saved
    else -> null
}

private fun NavTab.route(): Any = when (this) {
    NavTab.Home -> HomeRoute
    NavTab.Now -> NowRoute
    NavTab.Surprise -> SurpriseRoute
    NavTab.Map -> MapRoute
    NavTab.Saved -> SavedRoute
}

/** Cambia de pestaña conservando el estado de cada una y sin apilar destinos repetidos. */
fun NavHostController.navigateToTab(tab: NavTab) {
    navigate(tab.route()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Entra en la app principal (Inicio) borrando el onboarding o las pantallas previas. */
fun NavHostController.navigateToHomeClearingBackStack() {
    navigate(HomeRoute) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.headlineMedium)
    }
}
