package com.citypulse.ui.now

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExploreOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.NearbyEventCard
import com.citypulse.core.designsystem.component.PulseDot
import com.citypulse.core.designsystem.component.dashedBorder
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.OverlineStyle
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.TimeSlot
import com.citypulse.domain.model.UserPreferences
import com.citypulse.domain.usecase.nearbyInSlot
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun NowRoute(onOpenEvent: (String) -> Unit, viewModel: NowViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NowScreen(
        state = state,
        onOpenEvent = onOpenEvent,
        onSelectSlot = viewModel::selectSlot,
        onRadiusChange = viewModel::setRadius,
        onWiden = viewModel::widenRadius,
    )
}

@Composable
fun NowScreen(
    state: NowUiState,
    onOpenEvent: (String) -> Unit,
    onSelectSlot: (TimeSlot) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onWiden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulseDot(CpColors.Live)
                    Text("ESTOY AQUÍ AHORA", style = OverlineStyle, color = CpColors.LiveSoft)
                }
                Text("Cerca de ti", style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MyLocation, null, tint = CpColors.Cyan, modifier = Modifier.size(16.dp))
                    Text(
                        state.locationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                SlotSelector(state.slot, onSelectSlot, Modifier.padding(top = 18.dp))
                RadiusCard(state.radiusMinutes, onRadiusChange, Modifier.padding(top = 14.dp))
                Text(
                    state.countLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = CpColors.TextTertiary,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
        if (state.events.isEmpty() && !state.loading) {
            item { NothingNearby(state.radiusMinutes, state.canWiden, onWiden) }
        }
        items(state.events, key = { it.id }) { event ->
            NearbyEventCard(
                title = event.title,
                subtitle = "${event.timeRange} · ${event.venue}",
                walkMinutes = event.walkMinutes,
                imageUrl = event.imageUrl,
                fallback = event.fallback,
                live = event.live,
                free = event.free,
                onClick = { onOpenEvent(event.id) },
            )
        }
    }
}

private val TimeSlot.icon: ImageVector
    get() = when (this) {
        TimeSlot.Morning -> Icons.Rounded.WbTwilight
        TimeSlot.Afternoon -> Icons.Rounded.WbSunny
        TimeSlot.Night -> Icons.Rounded.DarkMode
    }

/** Selector segmentado Mañana / Tarde / Noche. */
@Composable
private fun SlotSelector(selected: TimeSlot, onSelect: (TimeSlot) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().glass(CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimeSlot.entries.forEach { slot ->
            val active = slot == selected
            Row(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .then(if (active) Modifier.background(BrandGradientHorizontal) else Modifier)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Tab) { onSelect(slot) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(slot.icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(slot.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

/** Tarjeta con el slider del radio caminando (5–30 min en pasos de 5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiusCard(radiusMinutes: Int, onRadiusChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    // Valor local para que el slider se mueva fluido; se guarda al soltar.
    var dragging by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragging.isNaN()) radiusMinutes.toFloat() else dragging
    val min = UserPreferences.MIN_RADIUS_MINUTES
    val max = UserPreferences.MAX_RADIUS_MINUTES

    GlassSurface(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                Text("Radio caminando", fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(start = 6.dp).weight(1f))
                Text("${shown.roundToInt()} min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            val colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = CpColors.Magenta,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            )
            Slider(
                value = shown,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    if (!dragging.isNaN()) onRadiusChange(dragging.roundToInt())
                    dragging = Float.NaN
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = (max - min) / 5 - 1,
                colors = colors,
                // Pulgar circular y pista fina, como el control del prototipo.
                thumb = { Box(Modifier.size(20.dp).background(Color.White, CircleShape)) },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(4.dp),
                        colors = colors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (min..max step 5).forEach { Text("$it", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)) }
            }
        }
    }
}

@Composable
private fun NothingNearby(radiusMinutes: Int, canWiden: Boolean, onWiden: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().dashedBorder(24.dp).padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.ExploreOff, null, tint = CpColors.Cyan, modifier = Modifier.size(32.dp))
        Text("Nada a menos de $radiusMinutes min", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Prueba otra franja o amplía el radio.",
            style = MaterialTheme.typography.bodySmall,
            color = CpColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (canWiden) {
            GradientButton("Ampliar a 30 min", onClick = onWiden, height = 40.dp, modifier = Modifier.padding(top = 14.dp))
        }
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun NowPreview() {
    val now = LocalDateTime.now().withHour(21)
    val events = SampleData.events(now.toLocalDate()).withDistanceFrom(SampleData.PuertaDelSol)
        .nearbyInSlot(now, TimeSlot.Night, 15).map { it.toUi(now) }
    CityPulseTheme {
        Box(Modifier.background(CpColors.Background)) {
            NowScreen(
                state = NowUiState(loading = false, locationLabel = "Tu ubicación, Madrid", slot = TimeSlot.Night, radiusMinutes = 15, events = events),
                onOpenEvent = {}, onSelectSlot = {}, onRadiusChange = {}, onWiden = {},
            )
        }
    }
}
