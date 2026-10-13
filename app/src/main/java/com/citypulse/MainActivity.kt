package com.citypulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.CityPulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityPulseTheme {
                CityPulsePlaceholder()
            }
        }
    }
}

@Composable
private fun CityPulsePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "CityPulse",
            style = MaterialTheme.typography.headlineMedium.merge(TextStyle(brush = BrandGradientHorizontal)),
        )
    }
}

@Preview
@Composable
private fun CityPulsePlaceholderPreview() {
    CityPulseTheme { CityPulsePlaceholder() }
}
