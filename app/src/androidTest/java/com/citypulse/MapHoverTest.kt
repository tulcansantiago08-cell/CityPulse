package com.citypulse

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.toUi
import com.citypulse.ui.map.MapScreen
import com.citypulse.ui.map.MapUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MapHoverTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = LocalDate.now().atTime(10, 0)
    // Un único evento para que su nombre solo aparezca en la etiqueta (la tarjeta muestra otro).
    private val events = SampleData.events(now.toLocalDate())
        .filter { it.id == "jazz" || it.id == "rastro" }
        .withDistanceFrom(SampleData.PuertaDelSol)
        .map { it.toUi(now) }

    @Test
    fun pasarElCursorSobreUnPinMuestraSuNombre() {
        compose.setContent {
            CityPulseTheme {
                MapScreen(
                    state = MapUiState(
                        loading = false,
                        cityName = "Madrid",
                        origin = com.citypulse.domain.model.GeoPoint(SampleData.PuertaDelSol.latitude, SampleData.PuertaDelSol.longitude),
                        events = events,
                        selectedId = "jazz",
                    ),
                    onOpenEvent = {},
                    onSelect = {},
                )
            }
        }
        compose.waitForIdle()
        // "El Rastro" no está seleccionado: su nombre aún no está en pantalla.
        compose.onAllNodesWithText("El Rastro").assertCountEquals(0)

        val rastro = events.first { it.id == "rastro" }
        compose.runOnUiThread {
            val map = findMapView(compose.activity.window.decorView)!!
            val point = map.projection.toPixels(GeoPoint(rastro.latitude, rastro.longitude), null)
            map.dispatchGenericMotionEvent(hover(MotionEvent.ACTION_HOVER_ENTER, point.x.toFloat(), point.y.toFloat()))
            map.dispatchGenericMotionEvent(hover(MotionEvent.ACTION_HOVER_MOVE, point.x.toFloat(), point.y.toFloat()))
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("El Rastro").assertCountEquals(1)

        // Al salir el cursor, la etiqueta desaparece.
        compose.runOnUiThread {
            val map = findMapView(compose.activity.window.decorView)!!
            map.dispatchGenericMotionEvent(hover(MotionEvent.ACTION_HOVER_EXIT, 0f, 0f))
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("El Rastro").assertCountEquals(0)
    }

    private fun hover(action: Int, x: Float, y: Float): MotionEvent {
        val time = SystemClock.uptimeMillis()
        return MotionEvent.obtain(time, time, action, x, y, 0).apply { source = InputDevice.SOURCE_MOUSE }
    }

    private fun findMapView(view: View): MapView? = when (view) {
        is MapView -> view
        is ViewGroup -> (0 until view.childCount).asSequence().mapNotNull { findMapView(view.getChildAt(it)) }.firstOrNull()
        else -> null
    }
}
