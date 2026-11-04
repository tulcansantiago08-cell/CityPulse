package com.citypulse.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.UserAccount
import com.citypulse.domain.repository.AuthException
import com.citypulse.domain.repository.AuthRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.usecase.DeleteAccount
import com.citypulse.domain.usecase.SignOut
import com.citypulse.ui.auth.AuthFormError
import com.citypulse.ui.auth.toFormError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val account: UserAccount? = null,
    val savedCount: Int = 0,
)

/** Estado del diálogo "Eliminar cuenta". */
data class DeleteAccountState(
    val running: Boolean = false,
    val error: AuthFormError? = null,
    val done: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    saved: SavedEventsRepository,
    private val signOut: SignOut,
    private val deleteAccount: DeleteAccount,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(auth.currentUser, saved.observeSavedIds()) { account, ids ->
        ProfileUiState(loading = false, account = account, savedCount = ids.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _deleteState = MutableStateFlow(DeleteAccountState())
    val deleteState: StateFlow<DeleteAccountState> = _deleteState.asStateFlow()

    fun signOut() {
        viewModelScope.launch { signOut.invoke() }
    }

    fun deleteAccount(password: String) {
        if (_deleteState.value.running) return
        if (password.isEmpty()) {
            _deleteState.value = DeleteAccountState(error = AuthFormError.InvalidCredentials)
            return
        }
        _deleteState.value = DeleteAccountState(running = true)
        viewModelScope.launch {
            _deleteState.value = try {
                deleteAccount.invoke(password)
                DeleteAccountState(done = true)
            } catch (e: AuthException) {
                DeleteAccountState(error = e.error.toFormError())
            }
        }
    }

    /** Al cerrar el diálogo se olvida el error o el resultado anterior. */
    fun resetDelete() {
        if (!_deleteState.value.running) _deleteState.value = DeleteAccountState()
    }
}
