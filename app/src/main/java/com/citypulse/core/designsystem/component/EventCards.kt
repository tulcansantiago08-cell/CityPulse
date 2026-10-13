package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors

private val CardShape = RoundedCornerShape(28.dp)
private val RowShape = RoundedCornerShape(24.dp)

/** Tarjeta grande del carrusel "Pasando ahora" (252 × 330 dp). */
@Composable
fun EventCardLarge(
    title: String,
    overline: String,
    walkMinutes: Int,
    venue: String,
    imageUrl: String?,
    free: Boolean,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = true,
    fallback: List<Color> = DefaultImageFallback,
) {
    Box(
        modifier
            .width(252.dp)
            .height(330.dp)
            .clip(CardShape)
            .clickable(onClick = onClick),
    ) {
        EventImage(imageUrl, Modifier.fillMaxSize(), fallback)
        Box(Modifier.fillMaxSize().background(scrimBrush(0f to 0.15f, 0.3f to 0f, 1f to 0.92f)))
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (live) LiveBadge()
            if (free) FreeBadge()
        }
        SaveButton(saved, onToggleSave, Modifier.align(Alignment.TopEnd).padding(10.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(overline, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = CpColors.TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            WalkAndVenue(walkMinutes, venue)
        }
    }
}

@Composable
private fun WalkAndVenue(walkMinutes: Int, venue: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text("$walkMinutes min", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
        Box(Modifier.padding(horizontal = 10.dp).size(3.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
        Text(venue, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Fila glass de la lista "Esta semana". */
@Composable
fun EventRowCard(
    whenLabel: String,
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallback: List<Color> = DefaultImageFallback,
) {
    GlassSurface(modifier.fillMaxWidth(), shape = RowShape, onClick = onClick) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            EventImage(imageUrl, Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)), fallback)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    whenLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium.merge(TextStyle(brush = BrandGradientHorizontal, letterSpacing = 0.5.sp)),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CpColors.TextTertiary, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 6.dp).size(20.dp),
            )
        }
    }
}

/** Tarjeta horizontal del modo "Estoy aquí ahora" (112 dp de alto), con los minutos a pie a la derecha. */
@Composable
fun NearbyEventCard(
    title: String,
    subtitle: String,
    walkMinutes: Int,
    imageUrl: String?,
    live: Boolean,
    free: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallback: List<Color> = DefaultImageFallback,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RowShape)
            .clickable(onClick = onClick),
    ) {
        EventImage(imageUrl, Modifier.fillMaxSize(), fallback)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0.35f to CpColors.Background.copy(alpha = 0.95f),
                    1f to CpColors.Background.copy(alpha = 0.35f),
                ),
            ),
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (live) LiveBadge(size = BadgeSize.Small)
                if (free) FreeBadge(size = BadgeSize.Small)
            }
            Column(Modifier.padding(end = 70.dp)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CpColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
        }
        GlassSurface(
            Modifier.align(Alignment.BottomEnd).padding(12.dp).size(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$walkMinutes", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 18.sp)
                Text("min", fontSize = 9.sp, color = CpColors.TextSecondary, lineHeight = 12.sp)
            }
        }
    }
}

/** Tarjeta de la cuadrícula de Guardados (220 dp de alto). */
@Composable
fun SavedEventCard(
    whenLabel: String,
    title: String,
    imageUrl: String?,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallback: List<Color> = DefaultImageFallback,
) {
    Box(
        modifier
            .height(220.dp)
            .clip(RowShape)
            .clickable(onClick = onClick),
    ) {
        EventImage(imageUrl, Modifier.fillMaxSize(), fallback)
        Box(Modifier.fillMaxSize().background(scrimBrush(0.35f to 0f, 1f to 0.92f)))
        SaveButton(saved = true, onToggle = onRemove, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), size = 34.dp)
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(whenLabel, fontSize = 10.sp, color = CpColors.TextSecondary)
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 16.sp),
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun EventCardsPreview() {
    CityPulseTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EventCardLarge(
                title = "Jazz de medianoche", overline = "Música · hasta 23:30", walkMinutes = 6, venue = "Café Central",
                imageUrl = null, free = false, saved = false, onToggleSave = {}, onClick = {},
                fallback = listOf(Color(0xFF5A1A4A), Color(0xFF1A0F2A)),
            )
            EventRowCard("Vie 16 · 22:00", "Cine de verano", "Conde Duque · 6 €", null, onClick = {})
            NearbyEventCard("Poesía slam en Lavapiés", "20:30–22:30 · La Escalera", 4, null, live = true, free = true, onClick = {})
        }
    }
}
