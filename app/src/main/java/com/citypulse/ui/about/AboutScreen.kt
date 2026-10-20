package com.citypulse.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citypulse.BuildConfig
import com.citypulse.R
import com.citypulse.core.designsystem.component.CityPulseLogo
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.OverlineStyle
import com.citypulse.ui.common.openUrl

private const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
private const val REPO_URL = "https://github.com/tulcansantiago08-cell/CityPulse"

/** Información de la app: fuentes de datos y créditos, uso de la IA y privacidad. */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick = onBack)
            Text(stringResource(R.string.about_open), style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(start = 12.dp))
        }
        CityPulseLogo(Modifier.padding(top = 24.dp), fontSize = 24.sp)
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), fontSize = 12.sp, color = CpColors.TextMuted, modifier = Modifier.padding(top = 4.dp))
        Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyLarge, color = CpColors.TextSecondary, modifier = Modifier.padding(top = 12.dp))

        Section(stringResource(R.string.about_data_title)) {
            Paragraph(stringResource(R.string.about_data_tm))
            Paragraph(stringResource(R.string.about_data_catalog))
            Paragraph(stringResource(R.string.about_data_osm))
            Paragraph(stringResource(R.string.about_data_images))
        }
        Section(stringResource(R.string.about_ai_title)) { Paragraph(stringResource(R.string.about_ai_body)) }
        Section(stringResource(R.string.about_privacy_title)) { Paragraph(stringResource(R.string.about_privacy_body)) }

        Link(stringResource(R.string.about_osm_link)) { context.openUrl(OSM_COPYRIGHT_URL) }
        Link(stringResource(R.string.about_code_link)) { context.openUrl(REPO_URL) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = OverlineStyle, color = CpColors.Cyan, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    GlassSurface(Modifier.padding(top = 12.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontSize = 14.sp, color = CpColors.Cyan, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = CpColors.Cyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun AboutPreview() {
    CityPulseTheme { AboutScreen(onBack = {}) }
}
