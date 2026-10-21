package com.citypulse.data.auth

import com.citypulse.domain.model.UserAccount
import com.citypulse.domain.repository.AuthError
import com.citypulse.domain.repository.AuthException
import com.citypulse.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

/** Cuenta con correo y contraseña en Firebase Authentication. */
@Singleton
class FirebaseAuthRepository @Inject constructor() : AuthRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // El nombre se fija después de crear la cuenta, y Firebase no vuelve a avisar: se fuerza una emisión.
    private val profileVersion = MutableStateFlow(0)

    override val currentUser: Flow<UserAccount?> = combine(authState(), profileVersion) { user, _ ->
        user?.toAccount()
    }.distinctUntilChanged()

    private fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Unit = mapErrors {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signUp(name: String, email: String, password: String): Unit = mapErrors {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
        user?.updateProfile(userProfileChangeRequest { displayName = name.trim() })?.await()
        profileVersion.update { it + 1 }
    }

    override suspend fun sendPasswordReset(email: String): Unit = mapErrors {
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun signOut() = auth.signOut()

    private inline fun <T> mapErrors(block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw AuthException(e.toAuthError(), e)
    }
}

private fun FirebaseUser.toAccount() = UserAccount(uid = uid, name = displayName, email = email.orEmpty())

private fun Exception.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailInUse
    is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
    // Con la protección contra enumeración de correos, Firebase responde igual a "no existe" y "contraseña mal".
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.InvalidEmail else AuthError.InvalidCredentials
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    is FirebaseNetworkException -> AuthError.Network
    else -> AuthError.Unknown
}
