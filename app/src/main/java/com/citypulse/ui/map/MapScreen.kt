package com.citypulse.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.BadgeSize
import com.citypulse.core.designsystem.component.EventImage
import com.citypulse.core.designsystem.component.FreeBadge
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.LiveBadge
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.ui.common.EventUi

@Composable
fun MapRoute(onOpenEvent: (String) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(state, onOpenEvent, viewModel::select)
}

@Composable
fun MapScreen(
    state: MapUiState,
    onOpenEvent: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf<HoveredPin?>(null) }
    Box(modifier.fillMaxSize().background(CpColors.SurfaceMap)) {
        val origin = state.origin
        val selected = state.selected
        if (origin != null) {
            OsmEventMap(
                origin = origin,
                showUser = state.usingUserLocation,
                events = state.events,
                selectedId = selected?.id,
                onSelect = onSelect,
                onHover = { hovered = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
        hovered?.let { pin ->
            state.events.firstOrNull { it.id == pin.eventId }?.let { event -> PinPreview(event, pin) }
        }

        GlassSurface(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            fill = CpColors.Surface.copy(alpha = 0.85f),
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Map, null, tint = CpColors.Cyan, modifier = Modifier.size(20.dp))
                Text(
                    pluralStringResource(R.plurals.map_count, state.events.size, state.cityName, state.events.size),
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
        ) {
            // Crédito obligatorio de OpenStreetMap.
            Text(
                stringResource(R.string.map_attribution),
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
            if (selected != null) SelectedEventCard(selected, onClick = { onOpenEvent(selected.id) })
        }
    }
}

/** Etiqueta que aparece sobre el pin al pasar el cursor: nombre, minutos a pie y estado. */
@Composable
private fun PinPreview(event: EventUi, pin: HoveredPin) {
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    Column(
        Modifier
            // Centrada en horizontal sobre el pin y apoyada encima de él.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = (pin.x - placeable.width / 2f).toInt().coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
                    val y = (pin.y - pin.radiusPx - placeable.height - gap).toInt().coerceAtLeast(0)
                    placeable.place(x, y)
                }
            }
            .widthIn(max = 240.dp)
            .glass(RoundedCornerShape(16.dp), fill = CpColors.Surface.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            event.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (event.live) LiveBadge(size = BadgeSize.Small, showDot = false)
            if (event.free) FreeBadge(size = BadgeSize.Small)
            Text(
                stringResource(R.string.map_walk_venue, event.walkMinutes, event.whenLabel),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectedEventCard(event: EventUi, onClick: () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        fill = CpColors.Surface.copy(alpha = 0.85f),
        onClick = onClick,
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EventImage(event.imageUrl, Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)), event.fallback)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (event.live) LiveBadge(size = BadgeSize.Small, showDot = false)
                    if (event.free) FreeBadge(size = BadgeSize.Small)
                }
                Text(
                    event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.map_walk_venue, event.walkMinutes, event.venue),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 6.dp).size(22.dp),
            )
        }
    }
}
