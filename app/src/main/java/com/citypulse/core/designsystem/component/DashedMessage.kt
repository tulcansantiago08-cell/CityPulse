package com.citypulse.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.CpColors

/** Borde discontinuo redondeado (estados vacíos dentro de listas). */
fun Modifier.dashedBorder(radius: Dp, color: Color = Color.White.copy(alpha = 0.15f)): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/** Caja con borde discontinuo y un mensaje centrado. */
@Composable
fun DashedMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.dashedBorder(28.dp).padding(20.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = CpColors.TextTertiary, textAlign = TextAlign.Center)
    }
}
