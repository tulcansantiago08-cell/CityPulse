package com.citypulse.ui.city

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.OverlineStyle
import com.citypulse.core.designsystem.theme.Poppins
import com.citypulse.data.city.StaticCityRepository
import com.citypulse.domain.model.City

@Composable
fun CityRoute(onBack: () -> Unit, onDone: () -> Unit, viewModel: CityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }
    CityScreen(state, onBack, viewModel::onQueryChange, viewModel::select, viewModel::useCurrentLocation)
}

@Composable
fun CityScreen(
    state: CityUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (City) -> Unit,
    onUseLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", onClick = onBack)
                Text("Elige ciudad", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(start = 12.dp))
            }
        }
        item { SearchField(state.query, onQueryChange, Modifier.padding(top = 14.dp)) }
        item { UseLocationRow(state.locating, state.locationError, onUseLocation) }
        item {
            Text(
                if (state.query.isBlank()) "CIUDADES POPULARES" else "RESULTADOS",
                style = OverlineStyle,
                color = CpColors.TextMuted,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
            )
        }
        if (state.cities.isEmpty()) {
            item {
                Text(
                    "No encontramos “${state.query}”. Prueba con otra ciudad.",
                    fontSize = 13.sp,
                    color = CpColors.TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp),
                )
            }
        }
        items(state.cities, key = { it.name }) { city ->
            CityRow(city, active = city.name == state.selected?.name, onClick = { onSelect(city) })
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(52.dp).glass(RoundedCornerShape(26.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = CpColors.TextTertiary, modifier = Modifier.size(20.dp))
        Box(Modifier.padding(start = 10.dp).weight(1f)) {
            if (query.isEmpty()) Text("Buscar ciudad", fontSize = 14.sp, color = CpColors.TextMuted)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = Color.White),
                cursorBrush = SolidColor(CpColors.Cyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UseLocationRow(locating: Boolean, error: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !locating, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(BrandGradient, CircleShape), contentAlignment = Alignment.Center) {
            if (locating) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text("Usar mi ubicación actual", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(
                if (error) "No pudimos detectar tu ubicación" else "Detecta la ciudad automáticamente",
                style = MaterialTheme.typography.bodySmall,
                color = if (error) CpColors.LiveSoft else Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun CityRow(city: City, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) CpColors.Cyan.copy(alpha = 0.10f) else CpColors.GlassSubtle, shape)
            .border(1.dp, if (active) CpColors.Cyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(city.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(city.country, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        }
        if (active) Icon(Icons.Rounded.CheckCircle, "Ciudad actual", tint = CpColors.Cyan, modifier = Modifier.size(22.dp))
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun CityPreview() {
    val cities = StaticCityRepository().popularCities()
    CityPulseTheme { CityScreen(CityUiState(cities = cities, selected = cities.first()), {}, {}, {}, {}) }
}
