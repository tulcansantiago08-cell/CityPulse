package com.citypulse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.usecase.happeningNow
import com.citypulse.domain.usecase.matches
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.toUi
import com.citypulse.ui.home.HomeScreen
import com.citypulse.ui.home.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule val compose = createComposeRule()

    private val now = LocalDate.now().atTime(21, 30)
    // Un evento gratis (tapeo) y uno de pago (jazz), ambos en curso a las 21:30. Solo dos para que
    // las dos tarjetas del carrusel (LazyRow) se compongan en pantalla.
    private val all = SampleData.events(now.toLocalDate())
        .filter { it.id == "sanil" || it.id == "jazz" }
        .withDistanceFrom(SampleData.PuertaDelSol)

    private fun state(freeOnly: Boolean) = HomeUiState(
        loading = false,
        cityName = "Madrid",
        greeting = Greeting.Evening,
        freeOnly = freeOnly,
        happeningNow = all.filter { it.event.matches(freeOnly, null) }.happeningNow(now).map { it.toUi(now) },
    )

    @Test
    fun filtroSoloGratisQuitaLasTarjetasDePago() {
        var freeOnly by mutableStateOf(false)
        compose.setContent {
            CityPulseTheme {
                HomeScreen(
                    state = state(freeOnly),
                    onOpenEvent = {}, onChangeCity = {}, onSelectCategory = {},
                    onToggleFreeOnly = { freeOnly = !freeOnly }, onToggleSave = {},
                )
            }
        }
        compose.onNodeWithText("Buenas noches,").assertIsDisplayed()
        compose.onAllNodesWithText("JAZZ DE MEDIANOCHE").assertCountEquals(1)
        compose.onAllNodesWithText("TAPEO EN SAN ILDEFONSO").assertCountEquals(1)

        compose.onNodeWithText("Solo gratis").performClick()

        compose.onAllNodesWithText("JAZZ DE MEDIANOCHE").assertCountEquals(0)
        compose.onAllNodesWithText("TAPEO EN SAN ILDEFONSO").assertCountEquals(1)
    }
}
