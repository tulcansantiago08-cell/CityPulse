package com.citypulse.ui.empty

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.citypulse.core.designsystem.component.EmptyState
import com.citypulse.core.designsystem.theme.CityPulseTheme

/** Pantalla tras denegar el permiso de ubicación. */
@Composable
fun LocationDeniedScreen(
    onEnableLocation: () -> Unit,
    onChooseCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Rounded.LocationOff,
        title = "Sin acceso a tu ubicación",
        message = "Necesitamos saber dónde estás para mostrarte eventos a pocos minutos a pie.",
        primaryAction = "Activar ubicación",
        onPrimary = onEnableLocation,
        secondaryAction = "Elegir ciudad manualmente",
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
        title = "Sin conexión",
        message = "No podemos cargar lo que pasa ahora. Tus eventos guardados siguen disponibles sin conexión.",
        primaryAction = "Reintentar",
        onPrimary = onRetry,
        secondaryAction = "Ver guardados",
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
