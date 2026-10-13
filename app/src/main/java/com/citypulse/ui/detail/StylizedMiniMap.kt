package com.citypulse.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CpColors

/**
 * Mapa ilustrado de la ficha (cuadrícula, calles, ruta punteada y pin), como en el prototipo.
 * No muestra la geografía real; sirve de vista rápida hasta abrir "Cómo llegar".
 */
@Composable
fun StylizedMiniMap(distanceLabel: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(shape)
            .background(CpColors.SurfaceMap)
            .border(1.dp, CpColors.GlassBorder, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = 22.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.04f)
            var x = 0f
            while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height)); x += cell }
            var y = 0f
            while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y)); y += cell }

            val road = Color(0xFF1F1C2B)
            rotate(-12f, pivot = Offset(size.width / 2, 75.dp.toPx())) {
                drawRect(road, Offset(-20.dp.toPx(), 70.dp.toPx()), androidx.compose.ui.geometry.Size(size.width + 40.dp.toPx(), 10.dp.toPx()))
            }
            rotate(20f, pivot = Offset(124.dp.toPx(), size.height / 2)) {
                drawRect(road, Offset(120.dp.toPx(), -20.dp.toPx()), androidx.compose.ui.geometry.Size(9.dp.toPx(), size.height + 40.dp.toPx()))
            }

            val user = Offset(69.dp.toPx(), 93.dp.toPx())
            val pin = Offset(223.dp.toPx().coerceAtMost(size.width - 40.dp.toPx()), 36.dp.toPx())
            drawLine(
                CpColors.Cyan, user, pin, strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
            drawCircle(Color.White, 7.dp.toPx(), user)
            drawCircle(CpColors.Cyan, 5.dp.toPx(), user)

            // Pin en forma de gota con el gradiente de marca.
            val r = 15.dp.toPx()
            val drop = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(pin.x - r, pin.y - r, pin.x + r, pin.y + r))
                moveTo(pin.x - r * 0.7f, pin.y + r * 0.7f)
                lineTo(pin.x, pin.y + r * 1.7f)
                lineTo(pin.x + r * 0.7f, pin.y + r * 0.7f)
                close()
            }
            drawPath(drop, CpColors.Magenta.copy(alpha = 0.35f), style = Stroke(6.dp.toPx()))
            drawPath(drop, BrandGradient)
            drawCircle(Color.White, 4.dp.toPx(), pin)
        }
        Text(
            distanceLabel,
            fontSize = 11.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .glass(CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
