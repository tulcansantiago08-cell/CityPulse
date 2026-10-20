package com.citypulse.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.PlainTextButton
import com.citypulse.core.designsystem.component.SavedEventCard
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.OverlineStyle
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime

@Composable
fun SavedRoute(onOpenEvent: (String) -> Unit, viewModel: SavedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SavedScreen(state, onOpenEvent, viewModel::remove, viewModel::clearPast)
}

@Composable
fun SavedScreen(
    state: SavedUiState,
    onOpenEvent: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearPast: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text(
                    pluralStringResource(R.plurals.saved_count, state.events.size, state.events.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CpColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (state.isEmpty && !state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { NothingSaved() }
        }
        items(state.events, key = { it.id }) { event ->
            SavedEventCard(
                whenLabel = event.whenLabel,
                title = event.title,
                imageUrl = event.imageUrl,
                fallback = event.fallback,
                onRemove = { onRemove(event.id) },
                onClick = { onOpenEvent(event.id) },
            )
        }
        if (state.past.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.saved_past), style = OverlineStyle, color = CpColors.TextMuted, modifier = Modifier.weight(1f))
                    PlainTextButton(stringResource(R.string.saved_clear_past), onClick = onClearPast)
                }
            }
            items(state.past, key = { it.id }) { event ->
                SavedEventCard(
                    whenLabel = stringResource(R.string.saved_ended) + " · " + event.absoluteWhen,
                    title = event.title,
                    imageUrl = event.imageUrl,
                    fallback = event.fallback,
                    onRemove = { onRemove(event.id) },
                    onClick = { onOpenEvent(event.id) },
                    modifier = Modifier.alpha(0.5f),
                )
            }
        }
    }
}

@Composable
private fun NothingSaved() {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Favorite, null, tint = CpColors.Magenta, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.saved_empty_title), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Text(
            stringResource(R.string.saved_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = CpColors.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun SavedPreview() {
    val now = LocalDateTime.now()
    val events = SampleData.events(now.toLocalDate()).withDistanceFrom(SampleData.PuertaDelSol).takeLast(3).map { it.toUi(now) }
    CityPulseTheme { SavedScreen(SavedUiState(false, events), {}, {}) }
}
