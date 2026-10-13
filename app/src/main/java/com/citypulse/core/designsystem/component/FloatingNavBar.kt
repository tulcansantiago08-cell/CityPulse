package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

/** Pestañas de la barra inferior. Sorpresa va en el botón central. */
enum class NavTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Inicio", Icons.Outlined.Home, Icons.Rounded.Home),
    Now("Ahora", Icons.Outlined.NearMe, Icons.Rounded.NearMe),
    Surprise("Sorpresa", Icons.Rounded.AutoAwesome, Icons.Rounded.AutoAwesome),
    Map("Mapa", Icons.Outlined.Map, Icons.Rounded.Map),
    Saved("Guardados", Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite),
}

/** Barra de navegación flotante tipo píldora glass con el botón Sorpresa elevado en el centro. */
@Composable
fun FloatingNavBar(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .glow(RoundedCornerShape(33.dp), color = Color.Black, elevation = 12.dp)
                .background(CpColors.Surface.copy(alpha = 0.92f), RoundedCornerShape(33.dp))
                .border(1.dp, CpColors.GlassBorder, RoundedCornerShape(33.dp))
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(NavTab.Home, selected, onSelect)
            NavItem(NavTab.Now, selected, onSelect)
            SurpriseButton(onClick = { onSelect(NavTab.Surprise) }, Modifier.offset(y = (-13).dp))
            NavItem(NavTab.Map, selected, onSelect)
            NavItem(NavTab.Saved, selected, onSelect)
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: NavTab, onSelect: (NavTab) -> Unit) {
    val active = tab == selected
    Column(
        Modifier
            .width(58.dp)
            .semantics { this.selected = active }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
            ) { onSelect(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = if (active) tab.selectedIcon else tab.icon,
            contentDescription = null,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(24.dp).then(if (active) Modifier.gradientTint() else Modifier),
        )
        Text(tab.label, fontSize = 10.sp, color = if (active) Color.White else Color.White.copy(alpha = 0.55f))
    }
}

@Composable
private fun SurpriseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(68.dp)
            .background(CpColors.Background, CircleShape)
            .padding(5.dp)
            .glow(CircleShape, elevation = 20.dp)
            .background(BrandGradient, CircleShape)
            .semantics { contentDescription = "Sorpresa" }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true, widthDp = 360, heightDp = 140)
@Composable
private fun FloatingNavBarPreview() {
    CityPulseTheme {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            FloatingNavBar(NavTab.Home, onSelect = {}, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
