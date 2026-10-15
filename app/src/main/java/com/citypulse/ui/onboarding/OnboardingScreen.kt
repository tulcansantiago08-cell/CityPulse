package com.citypulse.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.CityPulseLogo
import com.citypulse.core.designsystem.component.EventImage
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.LiveBadge
import com.citypulse.core.designsystem.component.PlainTextButton
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.ui.common.rememberLocationPermissionRequester

private fun unsplash(id: String) = "https://images.unsplash.com/$id?auto=format&fit=crop&w=720&q=70"

private val CollageShape = RoundedCornerShape(28.dp)
private val CollageHeight = 440.dp

@Composable
fun OnboardingRoute(
    onResult: (LocationSetupResult) -> Unit,
    onChooseCity: () -> Unit,
    viewModel: LocationSetupViewModel = hiltViewModel(),
) {
    val resolving by viewModel.resolving.collectAsStateWithLifecycle()
    val requester = rememberLocationPermissionRequester(viewModel::onPermissionResult)
    LaunchedEffect(viewModel) { viewModel.results.collect(onResult) }
    OnboardingScreen(
        onEnableLocation = { requester.request() },
        onChooseCity = onChooseCity,
        resolving = resolving,
    )
}

/** Pantalla de bienvenida: collage de imágenes, mensaje y acciones de ubicación. */
@Composable
fun OnboardingScreen(
    onEnableLocation: () -> Unit,
    onChooseCity: () -> Unit,
    modifier: Modifier = Modifier,
    resolving: Boolean = false,
) {
    Box(modifier.fillMaxSize().background(CpColors.Background)) {
        Box(Modifier.fillMaxWidth().height(CollageHeight)) {
            Row(
                Modifier.fillMaxSize().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EventImage(
                    unsplash("photo-1501386761578-eac5c94b800a"),
                    Modifier.weight(1.15f).fillMaxHeight().clip(CollageShape),
                    listOf(Color(0xFF5A1A4A), Color(0xFF1A0F2A)),
                )
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EventImage(
                        unsplash("photo-1488459716781-31db52582fe9"),
                        Modifier.weight(1f).fillMaxWidth().clip(CollageShape),
                        listOf(Color(0xFF4A2A1A), Color(0xFF100B0A)),
                    )
                    EventImage(
                        unsplash("photo-1518998053901-5348d3961a04"),
                        Modifier.weight(1f).fillMaxWidth().clip(CollageShape),
                        listOf(Color(0xFF1A3A5A), Color(0xFF0B0A10)),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.85f to CpColors.Background)),
            )
            LiveBadge(
                text = stringResource(R.string.onboarding_badge),
                modifier = Modifier.statusBarsPadding().padding(start = 30.dp, top = 36.dp),
            )
        }

        // El texto se monta 56 dp sobre el final del collage, como en el prototipo.
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = CollageHeight - 56.dp)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        ) {
            CityPulseLogo()
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = CpColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            GradientButton(
                text = stringResource(if (resolving) R.string.onboarding_locating else R.string.action_enable_location),
                onClick = onEnableLocation,
                enabled = !resolving,
                icon = Icons.Rounded.NearMe,
                modifier = Modifier.fillMaxWidth(),
            )
            PlainTextButton(
                stringResource(R.string.action_choose_city_manually),
                onClick = onChooseCity,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingPreview() {
    CityPulseTheme { OnboardingScreen(onEnableLocation = {}, onChooseCity = {}) }
}
