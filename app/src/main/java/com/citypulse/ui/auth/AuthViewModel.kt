package com.citypulse.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.repository.AuthError
import com.citypulse.domain.repository.AuthException
import com.citypulse.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SignIn, SignUp }

/** Errores que se muestran bajo el formulario: los de validación local y los de Firebase. */
enum class AuthFormError { NameRequired, InvalidEmail, ShortPassword, InvalidCredentials, EmailInUse, WeakPassword, TooManyRequests, Network, Unknown }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val error: AuthFormError? = null,
    /** Se envió el correo para restablecer la contraseña a esta dirección. */
    val resetSentTo: String? = null,
    /** La sesión ya está iniciada: la pantalla se cierra. */
    val done: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(mode = if (savedStateHandle.get<Boolean>("signUp") == true) AuthMode.SignUp else AuthMode.SignIn),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setMode(mode: AuthMode) = _uiState.update { it.copy(mode = mode, error = null, resetSentTo = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null, resetSentTo = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun togglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val state = _uiState.value
        if (state.loading) return
        validate(state)?.let { error -> return _uiState.update { it.copy(error = error) } }
        run {
            when (state.mode) {
                AuthMode.SignIn -> auth.signIn(state.email, state.password)
                AuthMode.SignUp -> auth.signUp(state.name, state.email, state.password)
            }
            _uiState.update { it.copy(done = true) }
        }
    }

    /** Envía el correo para elegir una contraseña nueva a la dirección escrita en el formulario. */
    fun resetPassword() {
        val email = _uiState.value.email.trim()
        if (!isValidEmail(email)) return _uiState.update { it.copy(error = AuthFormError.InvalidEmail) }
        run {
            auth.sendPasswordReset(email)
            _uiState.update { it.copy(resetSentTo = email) }
        }
    }

    private fun run(block: suspend () -> Unit) {
        _uiState.update { it.copy(loading = true, error = null, resetSentTo = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: AuthException) {
                _uiState.update { it.copy(error = e.error.toFormError()) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}

internal fun validate(state: AuthUiState): AuthFormError? = when {
    state.mode == AuthMode.SignUp && state.name.isBlank() -> AuthFormError.NameRequired
    !isValidEmail(state.email.trim()) -> AuthFormError.InvalidEmail
    state.password.length < MIN_PASSWORD -> AuthFormError.ShortPassword
    else -> null
}

/** Mínimo de Firebase Authentication. */
internal const val MIN_PASSWORD = 6

private val EmailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

internal fun isValidEmail(email: String) = EmailRegex.matches(email)

private fun AuthError.toFormError() = when (this) {
    AuthError.InvalidCredentials -> AuthFormError.InvalidCredentials
    AuthError.EmailInUse -> AuthFormError.EmailInUse
    AuthError.WeakPassword -> AuthFormError.WeakPassword
    AuthError.InvalidEmail -> AuthFormError.InvalidEmail
    AuthError.TooManyRequests -> AuthFormError.TooManyRequests
    AuthError.Network -> AuthFormError.Network
    AuthError.Unknown -> AuthFormError.Unknown
}
