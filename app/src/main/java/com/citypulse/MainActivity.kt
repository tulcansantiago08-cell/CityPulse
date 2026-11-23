package com.citypulse

import android.content.Context
import android.os.Bundle
import com.citypulse.domain.i18n.AppLocale
import com.citypulse.ui.language.LanguageManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.ui.navigation.CityPulseNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al volver a crearse tras un cambio de idioma, fechas y precios siguen al nuevo.
        AppLocale.language = LanguageManager.current(this)
        // La app siempre es oscura: iconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            CityPulseTheme {
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
                val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                val background = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                // Hasta leer las preferencias solo se pinta el fondo (dura unos milisegundos).
                startDestination?.let { CityPulseNavHost(startDestination = it, isOnline = isOnline, modifier = background) }
                    ?: Box(background)
            }
        }
    }
}
