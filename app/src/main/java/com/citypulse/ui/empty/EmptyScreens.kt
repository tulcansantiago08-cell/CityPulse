package com.citypulse.ui.empty

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.citypulse.R
import com.citypulse.core.designsystem.component.EmptyState
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.ui.common.hasLocationPermission
import com.citypulse.ui.common.rememberLocationPermissionRequester
import com.citypulse.ui.onboarding.LocationSetupResult
import com.citypulse.ui.onboarding.LocationSetupViewModel

@Composable
fun LocationDeniedRoute(
    onResult: (LocationSetupResult) -> Unit,
    onChooseCity: () -> Unit,
    viewModel: LocationSetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val requester = rememberLocationPermissionRequester(viewModel::onPermissionResult)
    LaunchedEffect(viewModel) { viewModel.results.collect(onResult) }
    // Al volver de los ajustes del sistema, si ya se concedió el permiso, se continúa.
    LifecycleResumeEffect(Unit) {
        if (context.hasLocationPermission()) viewModel.onPermissionResult(granted = true)
        onPauseOrDispose { }
    }
    LocationDeniedScreen(onEnableLocation = { requester.request(afterDenial = true) }, onChooseCity = onChooseCity)
}

/** Pantalla tras denegar el permiso de ubicación. */
@Composable
fun LocationDeniedScreen(
    onEnableLocation: () -> Unit,
    onChooseCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Rounded.LocationOff,
        title = stringResource(R.string.empty_location_title),
        message = stringResource(R.string.empty_location_body),
        primaryAction = stringResource(R.string.action_enable_location),
        onPrimary = onEnableLocation,
        secondaryAction = stringResource(R.string.action_choose_city_manually),
        onSecondary = onChooseCity,
        modifier = modifier,
    )
}

/**
 * Estado sin conexión para las pestañas que dependen de la red. Deja hueco abajo para
 * la barra de navegación, que sigue visible para poder ir a Guardados.
 */
@Composable
fun OfflineScreen(
    onRetry: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Rounded.WifiOff,
        title = stringResource(R.string.empty_offline_title),
        message = stringResource(R.string.empty_offline_body),
        primaryAction = stringResource(R.string.action_retry),
        onPrimary = onRetry,
        secondaryAction = stringResource(R.string.action_see_saved),
        onSecondary = onOpenSaved,
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 110.dp),
        modifier = modifier,
    )
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun LocationDeniedPreview() {
    CityPulseTheme { LocationDeniedScreen({}, {}) }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun OfflinePreview() {
    CityPulseTheme { OfflineScreen({}, {}) }
}
