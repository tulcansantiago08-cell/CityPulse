package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

/** Círculo tipo "story" para filtrar por categoría. El seleccionado lleva anillo con gradiente. */
@Composable
fun CategoryStory(
    label: String,
    icon: ImageVector,
    imageUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .border(2.dp, if (selected) BrandGradient else SolidColor(Color.White.copy(alpha = 0.14f)), CircleShape)
                .padding(2.dp)
                .border(3.dp, CpColors.Background, CircleShape)
                .padding(3.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            EventImage(imageUrl, Modifier.fillMaxSize(), listOf(Color(0xFF2A1A3A), Color(0xFF2A1A3A)))
            Box(Modifier.fillMaxSize().background(CpColors.Background.copy(alpha = 0.35f)))
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, fontSize = 11.sp, color = if (selected) Color.White else CpColors.TextTertiary)
    }
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun CategoryStoryPreview() {
    CityPulseTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CategoryStory("Todo", Icons.Rounded.Apps, null, selected = true, onClick = {})
            CategoryStory("Música", Icons.Rounded.MusicNote, null, selected = false, onClick = {})
        }
    }
}
