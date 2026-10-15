package com.citypulse.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.model.GeoPoint
import com.citypulse.ui.common.EventUi
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Las teselas estándar de OpenStreetMap son claras; este filtro las pasa a escala de grises,
 * las invierte y las oscurece para que el mapa encaje con el tema oscuro de la app.
 * Fila = factor·(255 − gris) + desplazamiento, con un toque azulado en el canal B.
 */
internal fun darkMapFilter(): ColorMatrixColorFilter {
    fun row(factor: Float, offset: Float) =
        floatArrayOf(-0.299f * factor, -0.587f * factor, -0.114f * factor, 0f, 255f * factor + offset)
    val r = row(0.42f, 6f)
    val g = row(0.40f, 5f)
    val b = row(0.48f, 14f)
    return ColorMatrixColorFilter(ColorMatrix(r + g + b + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
}

private const val DEFAULT_ZOOM = 15.0

internal fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        // Los servidores de teselas exigen identificar la app.
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
}

/** Mapa OpenStreetMap con los pines de eventos y el punto del usuario. */
@Composable
fun OsmEventMap(
    origin: GeoPoint,
    showUser: Boolean,
    events: List<EventUi>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            overlayManager.tilesOverlay.setColorFilter(darkMapFilter())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            setBackgroundColor(CpColors.SurfaceMap.toArgb())
            overlayManager.tilesOverlay.loadingBackgroundColor = CpColors.SurfaceMap.toArgb()
            overlayManager.tilesOverlay.loadingLineColor = CpColors.SurfaceMap.toArgb()
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(OsmPoint(origin.latitude, origin.longitude))
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Recentra al cambiar de ciudad.
    LaunchedEffect(origin) { mapView.controller.animateTo(OsmPoint(origin.latitude, origin.longitude)) }

    // Fotos de los pines, descargadas con Coil (usa su caché).
    val photos = remember { mutableStateMapOf<String, Bitmap>() }
    LaunchedEffect(events) {
        events.filter { it.imageUrl != null && it.id !in photos }.forEach { event ->
            val request = ImageRequest.Builder(context).data(event.imageUrl).size(160).allowHardware(false).build()
            (context.imageLoader.execute(request) as? SuccessResult)?.let { photos[event.id] = it.image.toBitmap() }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.removeAll { it is Marker }
            if (showUser) {
                map.overlays.add(
                    Marker(map).apply {
                        position = OsmPoint(origin.latitude, origin.longitude)
                        icon = userDotBitmap(density).toDrawable(map.resources)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                        setOnMarkerClickListener { _, _ -> true }
                    },
                )
            }
            // El seleccionado se añade el último para que quede encima.
            events.sortedBy { it.id == selectedId }.forEach { event ->
                val ring = when {
                    event.id == selectedId -> PinRing.Selected
                    event.live -> PinRing.Live
                    else -> PinRing.Normal
                }
                map.overlays.add(
                    Marker(map).apply {
                        position = OsmPoint(event.latitude, event.longitude)
                        icon = pinBitmap(density, photos[event.id], event.fallback, ring).toDrawable(map.resources)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setInfoWindow(null)
                        title = event.title
                        setOnMarkerClickListener { _, _ ->
                            onSelect(event.id)
                            true
                        }
                    },
                )
            }
            map.invalidate()
        },
    )
}
