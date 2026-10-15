package com.citypulse

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.citypulse.core.designsystem.component.FloatingNavBar
import com.citypulse.core.designsystem.component.NavTab
import com.citypulse.core.designsystem.theme.CityPulseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingNavBarTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun cambiarDePestanaYAbrirSorpresa() {
        var selected by mutableStateOf(NavTab.Home)
        compose.setContent { CityPulseTheme { FloatingNavBar(selected, onSelect = { selected = it }) } }

        compose.onNodeWithText("Ahora").performClick()
        assertEquals(NavTab.Now, selected)

        compose.onNodeWithText("Guardados").performClick()
        compose.onNodeWithText("Guardados").assertIsSelected()

        compose.onNodeWithContentDescription("Sorpresa").performClick()
        assertEquals(NavTab.Surprise, selected)
    }
}
