package com.citypulse.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citypulse.domain.model.UserAccount
import com.citypulse.domain.repository.AuthRepository
import com.citypulse.domain.repository.SavedEventsRepository
import com.citypulse.domain.usecase.SignOut
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val account: UserAccount? = null,
    val savedCount: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    saved: SavedEventsRepository,
    private val signOut: SignOut,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(auth.currentUser, saved.observeSavedIds()) { account, ids ->
        ProfileUiState(loading = false, account = account, savedCount = ids.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun signOut() {
        viewModelScope.launch { signOut.invoke() }
    }
}
