package com.citypulse.ui.surprise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ExploreOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.EmptyState
import com.citypulse.core.designsystem.component.EventImage
import com.citypulse.core.designsystem.component.FreeBadge
import com.citypulse.core.designsystem.component.GlassButton
import com.citypulse.core.designsystem.component.GlassTag
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.LiveBadge
import com.citypulse.core.designsystem.component.glow
import com.citypulse.core.designsystem.component.gradientTint
import com.citypulse.core.designsystem.component.scrimBrush
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.toUi
import java.time.LocalDateTime

@Composable
fun SurpriseRoute(
    onOpenEvent: (String) -> Unit,
    onSeeNearby: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: SurpriseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SurpriseScreen(state, onOpenEvent, viewModel::reroll, onSeeNearby, onGoHome)
}

@Composable
fun SurpriseScreen(
    state: SurpriseUiState,
    onOpenEvent: (String) -> Unit,
    onReroll: () -> Unit,
    onSeeNearby: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = state.event
    when {
        state.loading -> Box(modifier.fillMaxSize())
        event == null -> EmptyState(
            icon = Icons.Rounded.ExploreOff,
            title = stringResource(if (state.rollsLeft == 0) R.string.surprise_empty_no_rolls_title else R.string.surprise_empty_title),
            message = if (state.rollsLeft == 0) {
                pluralStringResource(R.plurals.surprise_empty_no_rolls_body, state.maxRolls, state.maxRolls)
            } else {
                stringResource(R.string.surprise_empty_body)
            },
            primaryAction = stringResource(R.string.surprise_see_nearby),
            onPrimary = onSeeNearby,
            secondaryAction = stringResource(R.string.surprise_go_home),
            onSecondary = onGoHome,
            modifier = modifier.navigationBarsPadding().padding(bottom = 80.dp),
        )
        else -> SurpriseCard(event, state, onOpenEvent, onReroll, modifier)
    }
}

@Composable
private fun SurpriseCard(
    event: EventUi,
    state: SurpriseUiState,
    onOpenEvent: (String) -> Unit,
    onReroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp)
            .glow(shape, color = Color.Black, elevation = 24.dp)
            .clip(shape),
    ) {
        EventImage(event.imageUrl, Modifier.fillMaxSize(), event.fallback)
        Box(Modifier.fillMaxSize().background(scrimBrush(0f to 0.45f, 0.25f to 0f, 0.45f to 0.15f, 1f to 0.96f)))

        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassTag(stringResource(R.string.surprise_tag), leading = {
                Icon(Icons.Rounded.AutoAwesome, null, tint = CpColors.Cyan, modifier = Modifier.size(16.dp))
            })
            GlassTag(stringResource(R.string.surprise_roll, state.rollNumber, state.maxRolls))
        }

        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (event.live) LiveBadge()
                if (event.free) FreeBadge()
            }
            Text(
                "${event.categoryLabel} · ${event.area}",
                fontSize = 12.sp,
                color = CpColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                event.title.uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
            state.explanation?.let { text ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.padding(top = 2.dp).size(14.dp).gradientTint())
                    Text(text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(start = 6.dp))
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Meta(Icons.Rounded.Schedule, event.timeRange)
                Meta(Icons.AutoMirrored.Rounded.DirectionsWalk, "${event.walkMinutes} min")
                Meta(Icons.Rounded.Sell, event.priceLabel)
            }
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val canRoll = state.rollsLeft > 0
                GlassButton(
                    text = if (canRoll) stringResource(R.string.surprise_reroll, state.rollsLeft) else stringResource(R.string.surprise_no_rolls),
                    onClick = onReroll,
                    icon = Icons.Rounded.Refresh,
                    enabled = canRoll,
                    modifier = Modifier.weight(1f),
                )
                GradientButton(stringResource(R.string.surprise_go), onClick = { onOpenEvent(event.id) }, modifier = Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
private fun Meta(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
        Text(text, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun SurprisePreview() {
    val now = LocalDateTime.now().withHour(22).withMinute(10)
    val event = SampleData.events(now.toLocalDate()).withDistanceFrom(SampleData.PuertaDelSol)[3].toUi(now)
    CityPulseTheme {
        SurpriseScreen(
            SurpriseUiState(loading = false, event = event, rollNumber = 1),
            onOpenEvent = {}, onReroll = {}, onSeeNearby = {}, onGoHome = {},
        )
    }
}
