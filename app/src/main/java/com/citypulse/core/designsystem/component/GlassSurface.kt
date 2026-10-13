package com.citypulse.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

/** Contenedor con fondo glass. Por defecto usa el radio de 24 dp de las tarjetas del prototipo. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    fill: Color = CpColors.Glass,
    borderColor: Color = CpColors.GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .glass(shape, fill, borderColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content,
    )
}

/** Botón circular glass con icono (atrás, buscar, compartir, guardar). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = CpColors.TextPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .glass(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Botón de guardar (corazón) usado en tarjetas y en la ficha. */
@Composable
fun SaveButton(
    saved: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    GlassIconButton(
        icon = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = if (saved) "Quitar de guardados" else "Guardar evento",
        onClick = onToggle,
        modifier = modifier,
        size = size,
        iconSize = size * 0.52f,
        tint = if (saved) CpColors.Magenta else CpColors.TextPrimary,
    )
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun GlassPreview() {
    CityPulseTheme {
        Box(Modifier.size(200.dp)) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", onClick = {})
            GlassIconButton(Icons.Rounded.Search, "Buscar", onClick = {}, modifier = Modifier.align(Alignment.TopEnd))
            SaveButton(saved = true, onToggle = {}, modifier = Modifier.align(Alignment.BottomStart))
            SaveButton(saved = false, onToggle = {}, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}
