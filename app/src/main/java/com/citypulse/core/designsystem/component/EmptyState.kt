package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

/** Estado vacío a pantalla completa: icono en anillo con gradiente, texto y dos acciones. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    primaryAction: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: String? = null,
    onSecondary: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 40.dp),
) {
    Column(
        modifier.fillMaxSize().padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(132.dp)
                .glow(CircleShape, elevation = 30.dp)
                .border(2.dp, BrandGradient, CircleShape)
                .padding(2.dp)
                .background(CpColors.Surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp).gradientTint(),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = CpColors.TextPrimary.copy(alpha = 0.68f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        GradientButton(primaryAction, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
        if (secondaryAction != null) {
            Spacer(Modifier.height(6.dp))
            PlainTextButton(secondaryAction, onClick = onSecondary)
        }
    }
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EmptyStatePreview() {
    CityPulseTheme {
        EmptyState(
            icon = Icons.Rounded.WifiOff,
            title = "Sin conexión",
            message = "No podemos cargar lo que pasa ahora. Tus eventos guardados siguen disponibles sin conexión.",
            primaryAction = "Reintentar",
            onPrimary = {},
            secondaryAction = "Ver guardados",
        )
    }
}
