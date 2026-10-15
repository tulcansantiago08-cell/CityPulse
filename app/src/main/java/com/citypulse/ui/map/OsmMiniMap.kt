package com.citypulse.ui.map

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import com.citypulse.R
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.model.GeoPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Minimapa estático de la ficha: el evento, el usuario (si se conoce su ubicación) y una
 * línea punteada entre ambos. Tocarlo abre la ruta a pie.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMiniMap(
    eventLocation: GeoPoint,
    origin: GeoPoint?,
    showUser: Boolean,
    fallback: List<Color>,
    distanceLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(24.dp)
    val paddingPx = with(density) { 36.dp.roundToPx() }
    val directionsLabel = stringResource(R.string.action_directions)

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            overlayManager.tilesOverlay.setColorFilter(darkMapFilter())
            overlayManager.tilesOverlay.loadingBackgroundColor = CpColors.SurfaceMap.toArgb()
            overlayManager.tilesOverlay.loadingLineColor = CpColors.SurfaceMap.toArgb()
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(false)
            isTilesScaledToDpi = true
            // Mapa fijo: los toques los recoge el contenedor para abrir "Cómo llegar".
            setOnTouchListener { _, _ -> true }
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    Box(
        modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(shape)
            .border(1.dp, CpColors.GlassBorder, shape)
            .clickable(role = Role.Button, onClickLabel = directionsLabel, onClick = onClick),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                val event = OsmPoint(eventLocation.latitude, eventLocation.longitude)
                val user = origin?.takeIf { showUser }?.let { OsmPoint(it.latitude, it.longitude) }
                map.overlays.clear()
                if (user != null) {
                    map.overlays.add(
                        Polyline(map).apply {
                            setPoints(listOf(user, event))
                            outlinePaint.color = CpColors.Cyan.toArgb()
                            outlinePaint.strokeWidth = 2 * density.density
                            outlinePaint.pathEffect = android.graphics.DashPathEffect(
                                floatArrayOf(6 * density.density, 5 * density.density), 0f,
                            )
                        },
                    )
                    map.overlays.add(marker(map, user, userDotBitmap(density.density).toDrawable(map.resources)))
                }
                map.overlays.add(
                    marker(map, event, pinBitmap(density.density, null, fallback, PinRing.Selected).toDrawable(map.resources)),
                )
                map.addOnFirstLayoutListener { _, _, _, _, _ ->
                    if (user != null) {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPoints(listOf(user, event)), false, paddingPx)
                        if (map.zoomLevelDouble > 17.0) map.controller.setZoom(17.0)
                    } else {
                        map.controller.setZoom(16.0)
                        map.controller.setCenter(event)
                    }
                }
                map.invalidate()
            },
        )
        Text(
            distanceLabel,
            fontSize = 11.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .glass(CircleShape, fill = CpColors.Surface.copy(alpha = 0.8f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun marker(map: MapView, point: OsmPoint, drawable: Drawable) = Marker(map).apply {
    position = point
    icon = drawable
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setInfoWindow(null)
    setOnMarkerClickListener { _, _ -> false }
}
