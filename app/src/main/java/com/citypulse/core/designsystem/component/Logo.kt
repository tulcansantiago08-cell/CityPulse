package com.citypulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.BrandGradientHorizontal
import com.citypulse.core.designsystem.theme.Poppins

/** Logotipo: punto con gradiente y "City" + "Pulse" en gradiente. */
@Composable
fun CityPulseLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 15.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .glow(CircleShape, elevation = 8.dp)
                .background(BrandGradient, CircleShape),
        )
        Text(
            buildAnnotatedString {
                append("City")
                withStyle(SpanStyle(brush = BrandGradientHorizontal)) { append("Pulse") }
            },
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            color = Color.White,
        )
    }
}
