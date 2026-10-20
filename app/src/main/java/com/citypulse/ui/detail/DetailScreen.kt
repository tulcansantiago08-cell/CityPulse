package com.citypulse.ui.detail

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.EmptyState
import com.citypulse.core.designsystem.component.EventImage
import com.citypulse.core.designsystem.component.FreeBadge
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.GlassTag
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.LiveBadge
import com.citypulse.core.designsystem.component.SaveButton
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.GeoPoint
import com.citypulse.domain.usecase.withDistanceFrom
import com.citypulse.ui.common.EventUi
import com.citypulse.ui.common.openUrl
import com.citypulse.ui.common.openWalkingDirections
import com.citypulse.ui.common.shareMessage
import com.citypulse.ui.common.shareText
import com.citypulse.ui.common.toUi
import com.citypulse.ui.map.OsmMiniMap
import java.time.LocalDateTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DetailRoute(onBack: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DetailScreen(
        state = state,
        saved = state.saved,
        onBack = onBack,
        onToggleSave = viewModel::toggleSaved,
        onShare = { state.event?.let { context.shareText(it.shareMessage()) } },
        onDirections = { state.event?.let { context.openWalkingDirections(it.latitude, it.longitude) } },
        onOpenEventPage = { state.event?.url?.let(context::openUrl) },
    )
}

private enum class SheetPosition { Collapsed, Expanded }

private val HeroHeight = 480.dp
private val SheetCollapsedTop = 380.dp
private val SheetExpandedTop = 120.dp

@Composable
fun DetailScreen(
    state: DetailUiState,
    saved: Boolean,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenEventPage: () -> Unit = {},
) {
    val event = state.event
    if (event == null) {
        if (!state.loading) {
            EmptyState(
                icon = Icons.Rounded.EventBusy,
                title = stringResource(R.string.detail_unavailable_title),
                message = stringResource(R.string.detail_unavailable_body),
                primaryAction = stringResource(R.string.action_back),
                onPrimary = onBack,
                modifier = modifier,
            )
        }
        return
    }

    Box(modifier.fillMaxSize()) {
        EventImage(event.imageUrl, Modifier.fillMaxWidth().height(HeroHeight), event.fallback)
        Box(
            Modifier.fillMaxWidth().height(HeroHeight).background(
                Brush.verticalGradient(
                    0f to CpColors.Background.copy(alpha = 0.5f),
                    0.25f to Color.Transparent,
                    0.6f to Color.Transparent,
                    1f to CpColors.Background.copy(alpha = 0.6f),
                ),
            ),
        )
        DetailSheet(event, state, onDirections, onOpenEventPage)
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick = onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassIconButton(Icons.Rounded.Share, stringResource(R.string.action_share), onClick = onShare, iconSize = 20.dp)
                SaveButton(saved, onToggleSave, size = 44.dp)
            }
        }
    }
}

/** Hoja glass que se arrastra entre dos alturas; tocar el asa también la alterna. */
@Composable
private fun DetailSheet(event: EventUi, state: DetailUiState, onDirections: () -> Unit, onOpenEventPage: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightPx = constraints.maxHeight
        val collapsedPx = with(density) { SheetCollapsedTop.toPx() }
        val expandedPx = with(density) { SheetExpandedTop.toPx() }
        val sheetState = remember {
            AnchoredDraggableState(
                initialValue = SheetPosition.Collapsed,
                anchors = DraggableAnchors {
                    SheetPosition.Collapsed at collapsedPx
                    SheetPosition.Expanded at expandedPx
                },
            )
        }
        LaunchedEffect(collapsedPx, expandedPx) {
            sheetState.updateAnchors(
                DraggableAnchors {
                    SheetPosition.Collapsed at collapsedPx
                    SheetPosition.Expanded at expandedPx
                },
            )
        }
        val scope = rememberCoroutineScope()
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        val sheetHeight: Dp = maxHeight - SheetExpandedTop
        val toggleLabel = stringResource(R.string.detail_toggle_sheet)

        Column(
            Modifier
                .offset { IntOffset(0, sheetState.offset.takeUnless { it.isNaN() }?.roundToInt() ?: collapsedPx.roundToInt()) }
                .fillMaxWidth()
                .height(sheetHeight)
                .background(CpColors.Surface.copy(alpha = 0.92f), sheetShape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)), sheetShape),
        ) {
            // Asa: zona de arrastre y toque.
            Box(
                Modifier
                    .fillMaxWidth()
                    .anchoredDraggable(
                        sheetState,
                        Orientation.Vertical,
                        flingBehavior = AnchoredDraggableDefaults.flingBehavior(sheetState, animationSpec = spring()),
                    )
                    .semantics { contentDescription = toggleLabel }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        scope.launch {
                            sheetState.animateTo(
                                if (sheetState.currentValue == SheetPosition.Collapsed) SheetPosition.Expanded else SheetPosition.Collapsed,
                            )
                        }
                    }
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(40.dp).height(4.dp).background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
            }

            // La parte visible de la hoja depende de su posición; el contenido se desplaza dentro.
            val visibleHeight = with(density) {
                (maxHeightPx - (sheetState.offset.takeUnless { it.isNaN() } ?: collapsedPx)).toDp()
            }
            Box(Modifier.fillMaxWidth().height(visibleHeight - 20.dp)) {
                SheetContent(event, state, onDirections, onOpenEventPage, Modifier.fillMaxSize())
                GradientButton(
                    text = stringResource(R.string.action_directions),
                    onClick = onDirections,
                    icon = Icons.Rounded.Directions,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetContent(
    event: EventUi,
    state: DetailUiState,
    onDirections: () -> Unit,
    onOpenEventPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 120.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (event.live) LiveBadge()
            if (event.free) FreeBadge()
            GlassTag(event.categoryLabel)
        }
        Text(
            event.title.uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "${event.venue} · ${event.area}",
            style = MaterialTheme.typography.bodyMedium,
            color = CpColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoCell(Icons.Rounded.Schedule, CpColors.Cyan, stringResource(R.string.detail_schedule), event.timeRange, Modifier.weight(1f))
            InfoCell(
                Icons.Rounded.Sell,
                CpColors.Magenta,
                stringResource(R.string.detail_price),
                event.priceLabel,
                Modifier.weight(1f),
                // Si el precio está en la web del evento, la celda la abre.
                onClick = if (event.priceOnline) onOpenEventPage else null,
            )
            InfoCell(Icons.AutoMirrored.Rounded.DirectionsWalk, CpColors.LiveSoft, stringResource(R.string.detail_walk), stringResource(R.string.now_radius_value, event.walkMinutes), Modifier.weight(1f))
        }
        Text(
            event.description,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.padding(top = 16.dp),
        )
        OsmMiniMap(
            eventLocation = GeoPoint(event.latitude, event.longitude),
            origin = state.origin,
            showUser = state.usingUserLocation,
            fallback = event.fallback,
            distanceLabel = event.distanceLabel,
            onClick = onDirections,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun InfoCell(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    GlassSurface(modifier, shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (onClick != null) CpColors.Cyan else Color.White,
                )
                if (onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = CpColors.Cyan,
                        modifier = Modifier.padding(start = 4.dp).size(14.dp),
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun DetailPreview() {
    val now = LocalDateTime.now().withHour(21).withMinute(30)
    val event = SampleData.events(now.toLocalDate()).withDistanceFrom(SampleData.PuertaDelSol).first().toUi(now)
    CityPulseTheme {
        DetailScreen(DetailUiState(false, event), saved = false, onBack = {}, onToggleSave = {}, onShare = {}, onDirections = {})
    }
}
