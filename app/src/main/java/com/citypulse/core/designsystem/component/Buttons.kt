package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

/** Botón principal con el gradiente de marca y resplandor magenta (54 dp de alto). */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .height(height)
            .glow(CircleShape)
            .clip(CircleShape)
            .background(BrandGradientHorizontal)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/** Botón secundario glass ("Otra · N" en Sorpresa). */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(CircleShape)
            .glass(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) CpColors.TextPrimary else CpColors.TextPrimary.copy(alpha = 0.4f)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

/** Botón de texto sin fondo ("Elegir ciudad manualmente"). */
@Composable
fun PlainTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 22.sp),
        color = CpColors.TextPrimary.copy(alpha = 0.8f),
    )
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true, widthDp = 360)
@Composable
private fun ButtonsPreview() {
    CityPulseTheme {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GradientButton("Activar ubicación", onClick = {}, icon = Icons.Rounded.NearMe, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            GlassButton("Otra · 4", onClick = {}, icon = Icons.Rounded.Refresh, modifier = Modifier.fillMaxWidth())
            PlainTextButton("Elegir ciudad manualmente", onClick = {})
        }
    }
}
