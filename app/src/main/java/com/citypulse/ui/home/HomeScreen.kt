package com.citypulse.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.CategoryStory
import com.citypulse.core.designsystem.component.CountPill
import com.citypulse.core.designsystem.component.DashedMessage
import com.citypulse.core.designsystem.component.EventCardLarge
import com.citypulse.core.designsystem.component.EventRowCard
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.SkeletonBlock
import com.citypulse.core.designsystem.component.dashedBorder
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.Category
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.CategoryOptions
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime

@Composable
fun HomeRoute(
    onOpenEvent: (String) -> Unit,
    onChangeCity: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenEvent = onOpenEvent,
        onChangeCity = onChangeCity,
        onSelectCategory = viewModel::selectCategory,
        onToggleFreeOnly = viewModel::toggleFreeOnly,
        onToggleSave = viewModel::toggleSaved,
        onRetry = viewModel::retry,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenEvent: (String) -> Unit,
    onChangeCity: () -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onToggleFreeOnly: () -> Unit,
    onToggleSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item { Header(state, onChangeCity) }
        item {
            LazyRow(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(CategoryOptions, key = { it.label }) { option ->
                    CategoryStory(
                        label = option.label,
                        icon = option.icon,
                        imageUrl = option.imageUrl,
                        selected = option.category == state.selectedCategory,
                        onClick = { onSelectCategory(option.category) },
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.home_happening_now), style = MaterialTheme.typography.titleMedium, color = Color.White)
                CountPill(state.happeningNow.size, Modifier.padding(start = 8.dp))
                Box(Modifier.weight(1f))
                FreeOnlyToggle(state.freeOnly, onToggleFreeOnly)
            }
        }
        item {
            when {
                state.loading -> CarouselSkeleton()
                state.error -> LoadError(onRetry)
                else -> HappeningNowRow(state.happeningNow, state.savedIds, onOpenEvent, onToggleSave)
            }
        }
        item {
            Text(
                stringResource(R.string.home_this_week),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
            )
        }
        if (state.loading) {
            items(3) { SkeletonBlock(RoundedCornerShape(24.dp), Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().height(88.dp)) }
        }
        if (state.thisWeek.isEmpty() && !state.loading && !state.error) {
            item {
                DashedMessage(
                    stringResource(R.string.home_nothing_this_week),
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                )
            }
        }
        items(state.thisWeek, key = { it.id }) { event ->
            EventRowCard(
                whenLabel = event.whenLabel,
                title = event.title,
                subtitle = "${event.venue} · ${event.priceLabel}",
                imageUrl = event.imageUrl,
                fallback = event.fallback,
                onClick = { onOpenEvent(event.id) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Header(state: HomeUiState, onChangeCity: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(state.dateLine, style = MaterialTheme.typography.bodyMedium, color = CpColors.TextTertiary)
            Text(
                "${state.greeting.text()},",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onChangeCity),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.cityName,
                    style = MaterialTheme.typography.headlineMedium.merge(TextStyle(brush = BrandGradientHorizontal)),
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.home_change_city), tint = CpColors.Cyan, modifier = Modifier.size(24.dp))
            }
        }
        GlassIconButton(Icons.Rounded.Search, contentDescription = stringResource(R.string.home_search_city), onClick = onChangeCity)
    }
}

@Composable
private fun HappeningNowRow(
    events: List<EventUi>,
    savedIds: Set<String>,
    onOpenEvent: (String) -> Unit,
    onToggleSave: (String) -> Unit,
) {
    if (events.isEmpty()) {
        DashedMessage(
            stringResource(R.string.home_nothing_live),
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(160.dp),
        )
        return
    }
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
    ) {
        items(events, key = { it.id }) { event ->
            EventCardLarge(
                title = event.title,
                overline = event.overline,
                walkMinutes = event.walkMinutes,
                venue = event.venue,
                imageUrl = event.imageUrl,
                fallback = event.fallback,
                live = event.live,
                free = event.free,
                saved = event.id in savedIds,
                onToggleSave = { onToggleSave(event.id) },
                onClick = { onOpenEvent(event.id) },
            )
        }
    }
}

@Composable
private fun CarouselSkeleton() {
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { SkeletonBlock(RoundedCornerShape(28.dp), Modifier.width(252.dp).height(330.dp)) }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().dashedBorder(28.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.home_load_error_title), fontSize = 15.sp, color = Color.White)
        Text(
            stringResource(R.string.home_load_error_body),
            style = MaterialTheme.typography.bodySmall,
            color = CpColors.TextTertiary,
            modifier = Modifier.padding(top = 4.dp),
        )
        GradientButton(stringResource(R.string.action_retry), onClick = onRetry, height = 40.dp, modifier = Modifier.padding(top = 14.dp))
    }
}

/** Interruptor "Solo gratis" con pista en gradiente cuando está activo. */
@Composable
private fun FreeOnlyToggle(checked: Boolean, onToggle: () -> Unit) {
    val knobOffset by animateDpAsState(if (checked) 20.dp else 2.dp, label = "knob")
    val trackColor by animateColorAsState(if (checked) Color.Transparent else Color.White.copy(alpha = 0.16f), label = "track")
    val stateText = stringResource(if (checked) R.string.state_on else R.string.state_off)
    Row(
        Modifier
            .clip(CircleShape)
            .semantics { stateDescription = stateText }
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.home_free_only), fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
        Box(
            Modifier
                .padding(start = 8.dp)
                .width(40.dp)
                .height(22.dp)
                .background(if (checked) BrandGradientHorizontal else SolidColor(trackColor), CircleShape),
        ) {
            Box(
                Modifier
                    .offset(x = knobOffset, y = 2.dp)
                    .size(18.dp)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun Greeting.text(): String = stringResource(
    when (this) {
        Greeting.Morning -> R.string.home_greeting_morning
        Greeting.Afternoon -> R.string.home_greeting_afternoon
        Greeting.Evening -> R.string.home_greeting_evening
    },
)

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun HomePreview() {
    val now = LocalDateTime.now().withHour(21).withMinute(4)
    val events = SampleData.events(now.toLocalDate()).withDistanceFrom(SampleData.PuertaDelSol)
    CityPulseTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                cityName = "Madrid",
                dateLine = "Martes 13 oct · 21:04",
                greeting = Greeting.Evening,
                happeningNow = events.take(4).map { it.toUi(now) },
                thisWeek = events.drop(6).map { it.toUi(now) },
            ),
            onOpenEvent = {}, onChangeCity = {}, onSelectCategory = {}, onToggleFreeOnly = {}, onToggleSave = {},
        )
    }
}
