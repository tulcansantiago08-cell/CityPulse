package com.citypulse.ui.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citypulse.R
import com.citypulse.core.designsystem.component.CityPulseLogo
import com.citypulse.core.designsystem.component.GlassIconButton
import com.citypulse.core.designsystem.component.GradientButton
import com.citypulse.core.designsystem.component.PlainTextButton
import com.citypulse.core.designsystem.component.glass
import com.citypulse.core.designsystem.theme.CityPulseTheme
import com.citypulse.core.designsystem.theme.CpColors
import com.citypulse.core.designsystem.theme.Poppins

@Composable
fun AuthRoute(onBack: () -> Unit, onDone: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    AuthScreen(
        state = state,
        onBack = onBack,
        onModeChange = viewModel::setMode,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::togglePasswordVisible,
        onSubmit = viewModel::submit,
        onForgotPassword = viewModel::resetPassword,
    )
}

/** Inicio de sesión y registro con correo y contraseña en una sola pantalla. */
@Composable
fun AuthScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val signUp = state.mode == AuthMode.SignUp
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
    ) {
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick = onBack)
        CityPulseLogo(Modifier.padding(top = 20.dp), fontSize = 22.sp)
        Text(
            stringResource(if (signUp) R.string.auth_title_sign_up else R.string.auth_title_sign_in),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = CpColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )

        ModeTabs(state.mode, onModeChange, Modifier.padding(top = 24.dp))

        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (signUp) {
                AuthField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.auth_name),
                    icon = Icons.Rounded.Person,
                    contentType = ContentType.PersonFullName,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    onImeAction = { focus.moveFocus(FocusDirection.Down) },
                )
            }
            AuthField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email),
                icon = Icons.Rounded.Email,
                contentType = ContentType.EmailAddress,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                onImeAction = { focus.moveFocus(FocusDirection.Down) },
            )
            AuthField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password),
                icon = Icons.Rounded.Lock,
                contentType = if (signUp) ContentType.NewPassword else ContentType.Password,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                onImeAction = { focus.clearFocus(); onSubmit() },
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    GlassIconButton(
                        if (state.passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        stringResource(if (state.passwordVisible) R.string.auth_hide_password else R.string.auth_show_password),
                        onClick = onTogglePassword,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                },
            )
            if (signUp) {
                Text(stringResource(R.string.auth_password_hint, MIN_PASSWORD), fontSize = 12.sp, color = CpColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
            }
        }

        state.error?.let { error ->
            Text(
                error.message(),
                fontSize = 13.sp,
                color = CpColors.LiveSoft,
                modifier = Modifier.padding(top = 14.dp, start = 4.dp),
            )
        }
        state.resetSentTo?.let { email ->
            Row(Modifier.padding(top = 14.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MarkEmailRead, null, tint = CpColors.Cyan, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.auth_reset_sent, email), fontSize = 13.sp, color = CpColors.Cyan, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Box(Modifier.padding(top = 24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            GradientButton(
                text = if (state.loading) "" else stringResource(if (signUp) R.string.auth_submit_sign_up else R.string.auth_submit_sign_in),
                onClick = { focus.clearFocus(); onSubmit() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        }
        if (!signUp) {
            PlainTextButton(
                stringResource(R.string.auth_forgot_password),
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }

        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.CloudDone, null, tint = CpColors.TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.auth_benefit), fontSize = 12.sp, color = CpColors.TextMuted, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ModeTabs(mode: AuthMode, onModeChange: (AuthMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(48.dp).clip(CircleShape).glass(CircleShape).padding(4.dp)) {
        AuthMode.entries.forEach { option ->
            val selected = option == mode
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(Color.White.copy(alpha = 0.14f)) else Modifier)
                    .clickable(role = Role.Tab) { onModeChange(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (option == AuthMode.SignIn) R.string.auth_tab_sign_in else R.string.auth_tab_sign_up),
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else CpColors.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    contentType: ContentType,
    keyboardOptions: KeyboardOptions,
    onImeAction: () -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(shape).glass(shape).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = CpColors.TextTertiary, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            if (value.isEmpty()) Text(label, fontSize = 14.sp, color = CpColors.TextMuted)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = Color.White),
                cursorBrush = SolidColor(CpColors.Cyan),
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth().semantics { this.contentType = contentType; contentDescription = label },
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun AuthFormError.message(): String = stringResource(
    when (this) {
        AuthFormError.NameRequired -> R.string.auth_error_name
        AuthFormError.InvalidEmail -> R.string.auth_error_email
        AuthFormError.ShortPassword, AuthFormError.WeakPassword -> R.string.auth_error_short_password
        AuthFormError.InvalidCredentials -> R.string.auth_error_credentials
        AuthFormError.EmailInUse -> R.string.auth_error_email_in_use
        AuthFormError.TooManyRequests -> R.string.auth_error_too_many
        AuthFormError.Network -> R.string.auth_error_network
        AuthFormError.Unknown -> R.string.auth_error_unknown
    },
    MIN_PASSWORD,
)

@Preview(widthDp = 360, heightDp = 800, backgroundColor = 0xFF0B0A10, showBackground = true)
@Composable
private fun AuthPreview() {
    CityPulseTheme {
        AuthScreen(AuthUiState(mode = AuthMode.SignUp, error = AuthFormError.EmailInUse), {}, {}, {}, {}, {}, {}, {}, {})
    }
}
