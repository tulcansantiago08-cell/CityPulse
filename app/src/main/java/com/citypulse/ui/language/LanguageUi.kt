package com.citypulse.ui.language

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.R
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.i18n.AppLanguage

/** Nombre del idioma escrito en ese mismo idioma: "Español", "English". */
val AppLanguage.nameRes: Int
    get() = when (this) {
        AppLanguage.ES -> R.string.language_es
        AppLanguage.EN -> R.string.language_en
    }

/** Fila "Idioma" con las dos opciones: la elegida resaltada. El cambio es inmediato. */
@Composable
fun LanguageRow(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    val current = activity?.let { LanguageManager.current(it) } ?: AppLanguage.DEFAULT
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Language, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.language_title),
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { language ->
                    val isCurrent = language == current
                    Text(
                        stringResource(language.nameRes),
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) Color.White else CpColors.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isCurrent) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                            .semantics { selected = isCurrent }
                            .clickable(role = Role.RadioButton) { activity?.let { LanguageManager.set(it, language) } }
                            .heightIn(min = 36.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Botón de texto con el otro idioma ("🌐 English"), para la bienvenida. */
@Composable
fun LanguageSwitchButton(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    Text(
        "🌐 " + stringResource(R.string.language_switch),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = CpColors.TextSecondary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button) {
                activity?.let {
                    val other = if (LanguageManager.current(it) == AppLanguage.ES) AppLanguage.EN else AppLanguage.ES
                    LanguageManager.set(it, other)
                }
            }
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
