package com.citypulse.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.DashedMessage
import com.citypulse.core.designsystem.component.EventRowCard
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.SkeletonBlock
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.component.gradientTint
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.OverlineStyle
import com.citypulse.core.designsystem.theme.Poppins
import com.citypulse.domain.model.SearchIntent
import com.citypulse.ui.common.formatMoney

@Composable
fun AiSearchRoute(onBack: () -> Unit, onOpenEvent: (String) -> Unit, viewModel: AiSearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AiSearchScreen(state, onBack, onOpenEvent, viewModel::onQueryChange, viewModel::search)
}

@Composable
fun AiSearchScreen(
    state: AiSearchUiState,
    onBack: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit: (String) -> Unit = { keyboard?.hide(); onSearch(it) }

    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick = onBack)
                QueryField(state.query, onQueryChange, submit, Modifier.padding(start = 10.dp).weight(1f))
            }
        }
        when (state.status) {
            AiSearchStatus.Idle -> item { Intro(state.cityName, submit) }
            AiSearchStatus.Thinking -> {
                item {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = CpColors.Cyan, strokeWidth = 2.dp)
                        Text(stringResource(R.string.ai_thinking), fontSize = 13.sp, color = CpColors.TextTertiary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                items(3) { SkeletonBlock(RoundedCornerShape(24.dp), Modifier.fillMaxWidth().height(88.dp)) }
            }
            AiSearchStatus.Done -> {
                item { Understood(state) }
                if (state.results.isEmpty()) {
                    item { DashedMessage(stringResource(R.string.ai_no_results), Modifier.fillMaxWidth().padding(top = 8.dp)) }
                }
                items(state.results, key = { it.id }) { event ->
                    EventRowCard(
                        whenLabel = event.whenLabel,
                        title = event.title,
                        subtitle = "${event.venue} · ${event.priceLabel}",
                        imageUrl = event.imageUrl,
                        fallback = event.fallback,
                        onClick = { onOpenEvent(event.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueryField(query: String, onQueryChange: (String) -> Unit, onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier.height(52.dp).glass(RoundedCornerShape(26.dp)).padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(stringResource(R.string.ai_ask_hint), fontSize = 14.sp, color = CpColors.TextMuted)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = Color.White),
                cursorBrush = SolidColor(CpColors.Cyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(query) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandGradient)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.ai_search_send)) { onSubmit(query) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.ai_search_send), tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Intro(cityName: String, onSuggestion: (String) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp).gradientTint())
            Text(stringResource(R.string.ai_search_overline), style = OverlineStyle, color = CpColors.Cyan, modifier = Modifier.padding(start = 6.dp))
        }
        Text(stringResource(R.string.ai_search_title), style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.ai_search_body, cityName),
            style = MaterialTheme.typography.bodyMedium,
            color = CpColors.TextTertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        listOf(R.string.ai_suggestion_1, R.string.ai_suggestion_2, R.string.ai_suggestion_3, R.string.ai_suggestion_4).forEach { res ->
            val text = stringResource(res)
            Text(
                "“$text”",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .glass(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button) { onSuggestion(text) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/** Lo que entendió la IA, como etiquetas, y el número de resultados. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Understood(state: AiSearchUiState) {
    Column(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        val intent = state.intent
        if (state.usedFallback) {
            Text(stringResource(R.string.ai_fallback), fontSize = 12.sp, color = CpColors.LiveSoft, modifier = Modifier.padding(bottom = 6.dp))
        }
        when {
            intent == null || intent.isEmpty -> Text(stringResource(R.string.ai_understood_nothing), fontSize = 12.sp, color = CpColors.TextTertiary)
            else -> {
                Text(stringResource(R.string.ai_understood), fontSize = 12.sp, color = CpColors.TextTertiary)
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    intent.labels(state.currency).forEach { label ->
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.glass(CircleShape, borderColor = CpColors.Cyan.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        Text(
            pluralStringResource(R.plurals.ai_results_count, state.results.size, state.results.size),
            fontSize = 13.sp,
            color = CpColors.TextTertiary,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun SearchIntent.labels(currency: String): List<String> = buildList {
    categories.forEach { add(it.label) }
    if (freeOnly) add(stringResource(R.string.ai_chip_free))
    maxPrice?.let { add(stringResource(R.string.ai_chip_max_price, formatMoney(it, currency))) }
    if (todayOnly) add(stringResource(R.string.ai_chip_today))
    timeSlot?.let { add(it.label) }
    if (liveNow) add(stringResource(R.string.ai_chip_live))
    maxWalkMinutes?.let { add(stringResource(R.string.ai_chip_walk, it)) }
    keywords.forEach { add("“$it”") }
}
