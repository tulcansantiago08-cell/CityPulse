package com.citypulse.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.EventAvailable
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
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
import com.citypulse.core.designsystem.component.PlainTextButton
import com.citypulse.core.designsystem.component.SkeletonBlock
import com.citypulse.core.designsystem.component.dashedBorder
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.component.gradientTint
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.ui.profile.ProfileViewModel
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.Category
import com.citypulse.domain.model.BudgetScale
import com.citypulse.domain.time.Greeting
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.CategoryOptions
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.formatMoney
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime

@Composable
fun HomeRoute(
    onOpenEvent: (String) -> Unit,
    onChangeCity: () -> Unit,
    onAsk: () -> Unit = {},
    onAccount: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by profileViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenEvent = onOpenEvent,
        onChangeCity = onChangeCity,
        onSelectCategory = viewModel::selectCategory,
        onToggleFreeOnly = viewModel::toggleFreeOnly,
        onToggleSave = viewModel::toggleSaved,
        onRetry = viewModel::retry,
        onSelectBudget = viewModel::selectBudgetStep,
        onPreviewBudget = viewModel::previewBudgetStep,
        onAsk = onAsk,
        onRefresh = viewModel::refresh,
        onAccount = onAccount,
        accountInitial = profile.account?.initial,
        onClearFilters = viewModel::clearFilters,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectBudget: (Int) -> Unit = {},
    onPreviewBudget: (Int) -> Unit = {},
    onAsk: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAccount: () -> Unit = {},
    accountInitial: String? = null,
    onClearFilters: () -> Unit = {},
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.refreshing,
                containerColor = CpColors.Surface,
                color = CpColors.Magenta,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item { Header(state, onChangeCity, onAccount, accountInitial) }
            item { AskBar(onAsk) }
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
            item { BudgetSlider(state.budgetStep, state.budgetScale, state.budgetCounts, state.unknownPriceCount, onPreviewBudget, onSelectBudget) }
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
                    else -> HappeningNowRow(state.happeningNow, state.savedIds, onOpenEvent, onToggleSave, state.filtersActive, onClearFilters)
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
                    EmptyWithFilters(
                        stringResource(R.string.home_nothing_this_week),
                        state.filtersActive,
                        onClearFilters,
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
}

@Composable
private fun Header(state: HomeUiState, onChangeCity: () -> Unit, onAccount: () -> Unit, accountInitial: String?) {
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
        AccountButton(accountInitial, onAccount)
        GlassIconButton(Icons.Rounded.Search, contentDescription = stringResource(R.string.home_search_city), onClick = onChangeCity)
    }
}

/** Avatar con la inicial si hay sesión iniciada; si no, icono de persona. */
@Composable
private fun AccountButton(initial: String?, onClick: () -> Unit) {
    val label = stringResource(R.string.account_open)
    if (initial == null) {
        GlassIconButton(Icons.Rounded.Person, contentDescription = label, onClick = onClick, iconSize = 22.dp)
    } else {
        Box(
            Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandGradient)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun HappeningNowRow(
    events: List<EventUi>,
    savedIds: Set<String>,
    onOpenEvent: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    filtersActive: Boolean = false,
    onClearFilters: () -> Unit = {},
) {
    if (events.isEmpty()) {
        EmptyWithFilters(
            stringResource(R.string.home_nothing_live),
            filtersActive,
            onClearFilters,
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

/** Acceso a la búsqueda con IA: parece un campo de texto y abre la pantalla de búsqueda. */
@Composable
private fun AskBar(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 20.dp, end = 20.dp, top = 16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .glass(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp).gradientTint())
        Text(stringResource(R.string.ai_ask_hint), fontSize = 14.sp, color = CpColors.TextTertiary, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Deslizador de presupuesto máximo, de "Solo gratis" a "Sin límite", en la moneda de la ciudad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSlider(
    step: Int?,
    scale: BudgetScale,
    counts: List<Int>,
    unknownPriceCount: Int,
    onPreview: (Int) -> Unit,
    onSelect: (Int) -> Unit,
) {
    val selected = step ?: scale.unlimitedStep
    // Mientras se arrastra, las listas se filtran al momento; al soltar se guarda el presupuesto.
    var dragging by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragging.isNaN()) selected else dragging.roundToInt()
    val valueLabel = budgetLabel(scale.maxFor(shown), scale.currency)
    val colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = CpColors.Magenta,
        inactiveTrackColor = Color.White.copy(alpha = 0.16f),
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.budget_title), fontSize = 13.sp, color = CpColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(
                valueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(brush = if (shown == scale.unlimitedStep) null else BrandGradientHorizontal),
                color = if (shown == scale.unlimitedStep) Color.White.copy(alpha = 0.7f) else Color.Unspecified,
            )
        }
        Slider(
            value = shown.toFloat(),
            onValueChange = {
                if (dragging.isNaN() || it.roundToInt() != dragging.roundToInt()) onPreview(it.roundToInt())
                dragging = it
            },
            onValueChangeFinished = {
                if (!dragging.isNaN()) onSelect(dragging.roundToInt())
                dragging = Float.NaN
            },
            valueRange = 0f..scale.unlimitedStep.toFloat(),
            steps = scale.unlimitedStep - 1,
            colors = colors,
            thumb = { Box(Modifier.size(22.dp).background(Color.White, CircleShape)) },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                )
            },
            modifier = Modifier.semantics { stateDescription = valueLabel },
        )
        Row(Modifier.fillMaxWidth().offset(y = (-6).dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.budget_free), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
            Text(stringResource(R.string.budget_unlimited), fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
        }
        counts.getOrNull(shown)?.let { count ->
            // Con "Sin límite" se ven todos, también los que no publican precio.
            BudgetResult(count, unknownPriceCount.takeIf { shown < scale.unlimitedStep } ?: 0)
        }
    }
}

/** Cuántos eventos de "Pasando ahora" y "Esta semana" caben en el presupuesto elegido. */
@Composable
private fun BudgetResult(count: Int, withoutPrice: Int) {
    val text = if (count == 0) stringResource(R.string.budget_none) else pluralStringResource(R.plurals.budget_results, count, count)
    Row(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .glass(RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (count == 0) Icons.Rounded.SearchOff else Icons.Rounded.EventAvailable,
            contentDescription = null,
            tint = if (count == 0) CpColors.LiveSoft else CpColors.Cyan,
            modifier = Modifier.size(18.dp),
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            if (withoutPrice > 0) {
                Text(
                    pluralStringResource(R.plurals.budget_without_price, withoutPrice, withoutPrice),
                    fontSize = 11.sp,
                    color = CpColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun budgetLabel(max: Double?, currency: String): String = when (max) {
    null -> stringResource(R.string.budget_unlimited)
    0.0 -> stringResource(R.string.budget_free_only)
    else -> stringResource(R.string.price_up_to, formatMoney(max, currency))
}

/** Mensaje de lista vacía; si hay filtros activos, ofrece quitarlos (pueden ser la causa). */
@Composable
private fun EmptyWithFilters(text: String, filtersActive: Boolean, onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.dashedBorder(28.dp).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, fontSize = 13.sp, color = CpColors.TextTertiary, textAlign = TextAlign.Center)
        if (filtersActive) {
            PlainTextButton(stringResource(R.string.home_clear_filters), onClick = onClearFilters)
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
    val stateText = stringResource(if (checked) R.string.toggle_on else R.string.toggle_off)
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
                    // Lambda: la animación solo vuelve a colocar el círculo, sin recomponer.
                    .offset { IntOffset(knobOffset.roundToPx(), 2.dp.roundToPx()) }
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
