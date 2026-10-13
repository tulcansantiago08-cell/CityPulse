package com.citypulse.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.Poppins

/** Punto que late con un halo expansivo (animación cpPulse del prototipo). */
@Composable
fun PulseDot(color: Color = Color.White, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseProgress",
    )
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // El halo crece hasta 6 dp más allá del punto durante el 70 % del ciclo y se desvanece.
                val phase = (progress / 0.7f).coerceAtMost(1f)
                val haloRadius = this.size.minDimension / 2 + 6.dp.toPx() * phase
                drawCircle(color.copy(alpha = 0.7f * (1f - phase)), radius = haloRadius)
            }
            .background(color, CircleShape),
    )
}

/** Tamaños de los badges: normal en tarjetas grandes y ficha, compacto en listas. */
enum class BadgeSize(val fontSize: TextUnit, val padding: PaddingValues, val dotPadding: PaddingValues) {
    Regular(10.sp, PaddingValues(horizontal = 10.dp, vertical = 5.dp), PaddingValues(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)),
    Small(9.sp, PaddingValues(horizontal = 8.dp, vertical = 3.dp), PaddingValues(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)),
}

private fun badgeTextStyle(size: BadgeSize) = TextStyle(
    fontFamily = Poppins,
    fontWeight = FontWeight.SemiBold,
    fontSize = size.fontSize,
    letterSpacing = 0.6.sp,
    lineHeight = size.fontSize * 1.4f,
)

/** Badge naranja "EN CURSO" con punto pulsante. */
@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.Regular,
    text: String = "EN CURSO",
    showDot: Boolean = true,
) {
    Row(
        modifier = modifier
            .background(CpColors.Live, CircleShape)
            .padding(if (showDot) size.dotPadding else size.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (size == BadgeSize.Small) 5.dp else 6.dp),
    ) {
        if (showDot) PulseDot()
        Text(text, style = badgeTextStyle(size), color = Color.White)
    }
}

/** Badge cian "GRATIS". */
@Composable
fun FreeBadge(modifier: Modifier = Modifier, size: BadgeSize = BadgeSize.Regular) {
    Text(
        text = "GRATIS",
        modifier = modifier
            .background(CpColors.Cyan, CircleShape)
            .padding(size.padding),
        style = badgeTextStyle(size),
        color = CpColors.Background,
    )
}

/** Etiqueta glass con texto (categoría en la ficha, "Tirada 1 de 5"). */
@Composable
fun GlassTag(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier
            .glass(CircleShape)
            .padding(horizontal = if (leading != null) 12.dp else 10.dp, vertical = if (leading != null) 6.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(
            text,
            style = TextStyle(fontFamily = Poppins, fontSize = if (leading != null) 11.sp else 10.sp, letterSpacing = 0.4.sp),
            color = Color.White,
        )
    }
}

/** Contador naranja suave junto a "Pasando ahora". */
@Composable
fun CountPill(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = count.toString(),
        modifier = modifier
            .background(CpColors.Live.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = TextStyle(fontFamily = Poppins, fontSize = 12.sp),
        color = CpColors.LiveSoft,
    )
}

@Preview(backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun BadgesPreview() {
    CityPulseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { LiveBadge(); FreeBadge() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { LiveBadge(size = BadgeSize.Small); FreeBadge(size = BadgeSize.Small) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { GlassTag("Música"); CountPill(6) }
        }
    }
}
