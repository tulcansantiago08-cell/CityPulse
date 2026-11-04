package com.citypulse.ui.directions

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.model.TravelMode
import com.citypulse.domain.model.TravelRoute
import com.citypulse.domain.usecase.formatTravelTime
import com.citypulse.ui.common.fallbackColors
import com.citypulse.ui.common.hasLocationPermission
import com.citypulse.ui.common.openDirections
import com.citypulse.ui.common.rememberLocationPermissionRequester
import com.citypulse.ui.map.PinRing
import com.citypulse.ui.map.configureOsmdroid
import com.citypulse.ui.map.darkMapFilter
import com.citypulse.ui.map.pinBitmap
import com.citypulse.ui.map.userDotBitmap
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun DirectionsRoute(onBack: () -> Unit, viewModel: DirectionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLocationPermissionRequester { viewModel.retryLocation() }
    // Sin permiso no hay ruta posible: se pide al entrar.
    LaunchedEffect(Unit) { if (!context.hasLocationPermission()) permission.request() }
    // Al volver de los ajustes del sistema con el permiso ya concedido.
    LifecycleResumeEffect(state.locationUnavailable) {
        if (state.locationUnavailable && context.hasLocationPermission()) viewModel.retryLocation()
        onPauseOrDispose {}
    }
    DirectionsScreen(
        state = state,
        missingPermission = !context.hasLocationPermission(),
        onBack = onBack,
        onSelectMode = viewModel::selectMode,
        onRequestPermission = { permission.request(afterDenial = true) },
        onOpenGoogleMaps = {
            state.destination?.let {
                context.openDirections(it.location.latitude, it.location.longitude, state.selectedMode, it.placeQuery)
            }
        },
    )
}

/**
 * Ruta hasta el evento en un mapa a pantalla completa: el camino por las calles en cada modo, el
 * punto del usuario moviéndose en vivo, el tiempo, la distancia y un botón para seguir en Google Maps.
 */
@Composable
fun DirectionsScreen(
    state: DirectionsUiState,
    missingPermission: Boolean,
    onBack: () -> Unit,
    onSelectMode: (TravelMode) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val destination = state.destination
        val cardHeight = remember { mutableIntStateOf(0) }
        if (destination != null) {
            RouteMap(state, destination, cardHeight.intValue, Modifier.fillMaxSize())
        }

        Row(
            Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick = onBack)
        }

        if (destination != null) {
            RouteCard(
                state = state,
                destination = destination,
                missingPermission = missingPermission,
                onSelectMode = onSelectMode,
                onRequestPermission = onRequestPermission,
                onOpenGoogleMaps = onOpenGoogleMaps,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { cardHeight.intValue = it.height }
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        } else if (!state.loading) {
            Text(
                stringResource(R.string.detail_unavailable_title),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun RouteCard(
    state: DirectionsUiState,
    destination: Destination,
    missingPermission: Boolean,
    onSelectMode: (TravelMode) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), fill = CpColors.Surface.copy(alpha = 0.92f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                destination.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(destination.venue, fontSize = 13.sp, color = CpColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TravelMode.entries.forEach { mode ->
                    ModeTab(
                        mode = mode,
                        route = state.routes[mode],
                        selected = mode == state.selectedMode,
                        onClick = { onSelectMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                val route = state.route
                val summary = when {
                    state.arrived -> stringResource(R.string.directions_arrived)
                    state.selectedMode == TravelMode.TRANSIT -> stringResource(R.string.directions_transit_summary)
                    state.locationUnavailable -> stringResource(R.string.directions_no_location)
                    route == null -> stringResource(R.string.directions_locating)
                    route.exact -> stringResource(R.string.directions_summary, formatTravelTime(route.minutes), formatDistance(route.distanceMeters))
                    else -> stringResource(R.string.directions_straight_summary, formatDistance(route.distanceMeters))
                }
                Text(summary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                if (state.calculating) {
                    CircularProgressIndicator(color = CpColors.Cyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
            RouteNote(state)?.let { Text(it, fontSize = 12.sp, color = CpColors.TextTertiary) }

            if (state.locationUnavailable && missingPermission) {
                GradientButton(
                    stringResource(R.string.directions_allow_location),
                    onClick = onRequestPermission,
                    icon = Icons.Rounded.MyLocation,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }
            GradientButton(
                stringResource(
                    if (state.selectedMode == TravelMode.TRANSIT) R.string.directions_open_transit else R.string.directions_open_google_maps,
                ),
                onClick = onOpenGoogleMaps,
                icon = Icons.Rounded.Navigation,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(stringResource(R.string.directions_attribution), fontSize = 10.sp, color = CpColors.TextMuted)
        }
    }
}

@Composable
private fun ModeTab(mode: TravelMode, route: TravelRoute?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, label) = when (mode) {
        TravelMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk to R.string.directions_mode_walk
        TravelMode.BIKE -> Icons.Rounded.PedalBike to R.string.directions_mode_bike
        TravelMode.CAR -> Icons.Rounded.DirectionsCar to R.string.directions_mode_car
        TravelMode.TRANSIT -> Icons.Rounded.DirectionsBus to R.string.directions_mode_transit
    }
    ModeTabContent(
        icon = icon,
        description = stringResource(label),
        time = route?.takeIf { it.exact }?.let { formatTravelTime(it.minutes) } ?: "—",
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ModeTabContent(
    icon: ImageVector,
    description: String,
    time: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) CpColors.Cyan else CpColors.TextTertiary
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CpColors.Cyan.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp))
        Text(time, fontSize = 11.sp, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Aclaración bajo el resumen: la hora estimada de llegada o por qué no hay ruta. */
@Composable
private fun RouteNote(state: DirectionsUiState): String? {
    val route = state.route
    return when {
        route == null || state.arrived || state.calculating -> null
        !route.reachable -> stringResource(R.string.directions_no_road)
        !route.exact -> stringResource(R.string.directions_approximate)
        else -> {
            val arrival = LocalTime.now().plusMinutes(route.minutes.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
            stringResource(
                if (state.selectedMode == TravelMode.CAR) R.string.directions_arrival_no_traffic else R.string.directions_arrival,
                arrival,
            )
        }
    }
}

/** "350 m" o "1,4 km". */
private fun formatDistance(meters: Double): String =
    if (meters < 1_000) "${(meters / 10).roundToInt() * 10} m"
    else "%.1f km".format(java.util.Locale.forLanguageTag("es"), meters / 1_000)

@Composable
private fun RouteMap(state: DirectionsUiState, destination: Destination, bottomInsetPx: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val paddingPx = with(LocalDensity.current) { 64.dp.roundToPx() }
    // El encuadre se ajusta a la ruta solo cuando llega una ruta nueva; después el usuario mueve el mapa libremente.
    val fittedRoute = remember { mutableStateOf<List<GeoPoint>?>(null) }
    val following = remember { mutableStateOf(false) }

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            overlayManager.tilesOverlay.setColorFilter(darkMapFilter())
            overlayManager.tilesOverlay.loadingBackgroundColor = CpColors.SurfaceMap.toArgb()
            overlayManager.tilesOverlay.loadingLineColor = CpColors.SurfaceMap.toArgb()
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(16.0)
            controller.setCenter(OsmPoint(destination.location.latitude, destination.location.longitude))
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                val target = destination.location.toOsm()
                val user = state.user?.toOsm()
                val route = state.route
                map.overlays.clear()
                if (route != null) {
                    map.overlays.add(
                        Polyline(map).apply {
                            setPoints(route.points.map { it.toOsm() })
                            outlinePaint.color = CpColors.Cyan.toArgb()
                            outlinePaint.strokeWidth = 6 * density
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                            if (!route.exact) outlinePaint.pathEffect = DashPathEffect(floatArrayOf(8 * density, 7 * density), 0f)
                        },
                    )
                }
                map.overlays.add(
                    marker(map, target, pinBitmap(density, null, destination.category.fallbackColors(), PinRing.Selected).toDrawable(map.resources)),
                )
                if (user != null) map.overlays.add(marker(map, user, userDotBitmap(density).toDrawable(map.resources)))

                // Encuadra la ruta completa cada vez que se calcula una nueva.
                if (route != null && fittedRoute.value != route.points) {
                    fittedRoute.value = route.points
                    val box = BoundingBox.fromGeoPoints(route.points.map { it.toOsm() } + listOfNotNull(user, target))
                    map.post {
                        map.zoomToBoundingBox(box.extendedBelow(bottomInsetPx + paddingPx, map.height), true, paddingPx)
                        if (map.zoomLevelDouble > 18.0) map.controller.setZoom(18.0)
                    }
                } else if (following.value && user != null) {
                    map.controller.animateTo(user)
                }
                map.invalidate()
            },
        )

        if (state.user != null) {
            GlassIconButton(
                Icons.Rounded.MyLocation,
                stringResource(R.string.directions_center_me),
                onClick = {
                    following.value = true
                    state.user?.let { mapView.controller.animateTo(it.toOsm(), 17.5, 600L) }
                },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
                tint = CpColors.Cyan,
            )
        }
    }
}

/** Amplía el encuadre hacia el sur para que la ruta quede en la parte del mapa que la tarjeta no tapa. */
private fun BoundingBox.extendedBelow(insetPx: Int, mapHeightPx: Int): BoundingBox {
    if (mapHeightPx <= 0) return this
    val covered = (insetPx.toDouble() / mapHeightPx).coerceIn(0.0, 0.8)
    val south = latNorth - (latNorth - latSouth) / (1 - covered)
    return BoundingBox(latNorth, lonEast, south.coerceAtLeast(-85.0), lonWest)
}

private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)

private fun marker(map: MapView, point: OsmPoint, drawable: Drawable) = Marker(map).apply {
    position = point
    icon = drawable
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setInfoWindow(null)
    setOnMarkerClickListener { _, _ -> false }
}
