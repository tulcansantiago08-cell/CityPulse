package com.citypulse.ui.directions

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.Drawable
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
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.model.GeoPoint
import com.citypulse.ui.common.fallbackColors
import com.citypulse.ui.common.openWalkingDirections
import com.citypulse.ui.map.PinRing
import com.citypulse.ui.map.configureOsmdroid
import com.citypulse.ui.map.darkMapFilter
import com.citypulse.ui.map.pinBitmap
import com.citypulse.ui.map.userDotBitmap
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
    DirectionsScreen(
        state = state,
        onBack = onBack,
        onOpenGoogleMaps = {
            state.destination?.location?.let { context.openWalkingDirections(it.latitude, it.longitude) }
        },
    )
}

/**
 * Ruta a pie hasta el evento en un mapa a pantalla completa: el camino por las calles, el punto
 * del usuario moviéndose en vivo, la distancia y los minutos, y un botón para seguir en Google Maps.
 */
@Composable
fun DirectionsScreen(
    state: DirectionsUiState,
    onBack: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val destination = state.destination
        if (destination != null) {
            RouteMap(state, destination, Modifier.fillMaxSize())
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
                onOpenGoogleMaps = onOpenGoogleMaps,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
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

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, tint = CpColors.Cyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                val route = state.route
                val summary = when {
                    state.arrived -> stringResource(R.string.directions_arrived)
                    state.locationUnavailable -> stringResource(R.string.directions_no_location)
                    route == null -> stringResource(R.string.directions_locating)
                    else -> stringResource(R.string.directions_summary, route.minutes, formatDistance(route.distanceMeters))
                }
                Text(summary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
                if (state.calculating) {
                    CircularProgressIndicator(color = CpColors.Cyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
            if (state.route?.exact == false && !state.calculating && !state.arrived) {
                Text(stringResource(R.string.directions_approximate), fontSize = 12.sp, color = CpColors.TextTertiary)
            }

            GradientButton(
                stringResource(R.string.directions_open_google_maps),
                onClick = onOpenGoogleMaps,
                icon = Icons.Rounded.Navigation,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(stringResource(R.string.directions_attribution), fontSize = 10.sp, color = CpColors.TextMuted)
        }
    }
}

/** "350 m" o "1,4 km". */
private fun formatDistance(meters: Double): String =
    if (meters < 1_000) "${(meters / 10).roundToInt() * 10} m"
    else "%.1f km".format(java.util.Locale.forLanguageTag("es"), meters / 1_000)

@Composable
private fun RouteMap(state: DirectionsUiState, destination: Destination, modifier: Modifier = Modifier) {
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
                        map.zoomToBoundingBox(box, true, paddingPx)
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

private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)

private fun marker(map: MapView, point: OsmPoint, drawable: Drawable) = Marker(map).apply {
    position = point
    icon = drawable
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setInfoWindow(null)
    setOnMarkerClickListener { _, _ -> false }
}
