package com.citypulse.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.GlassButton
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GlassSurface
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.theme.BrandGradient
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.domain.model.UserAccount

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onAbout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state, onBack, onSignIn, onSignUp, onAbout, viewModel::signOut)
}

/** Cuenta del usuario: invita a iniciar sesión o muestra el perfil con la opción de cerrarla. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onAbout: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
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
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(start = 12.dp))
        }

        val account = state.account
        when {
            state.loading -> Unit
            account == null -> SignedOut(onSignIn, onSignUp)
            else -> SignedIn(account, state.savedCount, onSignOut = { confirmSignOut = true })
        }

        MenuRow(Icons.Rounded.Info, stringResource(R.string.about_open), onAbout, Modifier.padding(top = 12.dp))
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_body)) },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) {
                    Text(stringResource(R.string.profile_sign_out), color = CpColors.LiveSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.action_cancel), color = Color.White) }
            },
            containerColor = CpColors.Surface,
            titleContentColor = Color.White,
            textContentColor = CpColors.TextSecondary,
        )
    }
}

@Composable
private fun SignedOut(onSignIn: () -> Unit, onSignUp: () -> Unit) {
    GlassSurface(Modifier.padding(top = 24.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).background(BrandGradient, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CloudDone, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text(
                stringResource(R.string.profile_signed_out_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                stringResource(R.string.profile_signed_out_body),
                style = MaterialTheme.typography.bodyMedium,
                color = CpColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            GradientButton(stringResource(R.string.auth_submit_sign_in), onClick = onSignIn, modifier = Modifier.padding(top = 18.dp).fillMaxWidth())
            GlassButton(stringResource(R.string.auth_tab_sign_up), onClick = onSignUp, modifier = Modifier.padding(top = 10.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun SignedIn(account: UserAccount, savedCount: Int, onSignOut: () -> Unit) {
    Column(Modifier.padding(top = 28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp).background(BrandGradient, CircleShape), contentAlignment = Alignment.Center) {
            Text(account.initial, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Text(
            account.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(account.email, style = MaterialTheme.typography.bodyMedium, color = CpColors.TextTertiary, modifier = Modifier.padding(top = 2.dp))
    }
    GlassSurface(Modifier.padding(top = 24.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoLine(Icons.Rounded.Favorite, CpColors.Magenta, pluralStringResource(R.plurals.profile_saved_count, savedCount, savedCount))
            InfoLine(Icons.Rounded.CloudDone, CpColors.Cyan, stringResource(R.string.profile_synced))
        }
    }
    MenuRow(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.profile_sign_out), onSignOut, Modifier.padding(top = 12.dp), tint = CpColors.LiveSoft)
}

@Composable
private fun InfoLine(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.White) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(text, fontSize = 14.sp, color = tint, modifier = Modifier.padding(start = 12.dp).weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CpColors.TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun ProfilePreview() {
    CityPulseTheme {
        ProfileScreen(ProfileUiState(false, UserAccount("1", "Santiago", "santiago@example.com"), 4), {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun ProfileSignedOutPreview() {
    CityPulseTheme { ProfileScreen(ProfileUiState(false), {}, {}, {}, {}, {}) }
}
