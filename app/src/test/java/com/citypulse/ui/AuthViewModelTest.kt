package com.citypulse.ui

import androidx.lifecycle.SavedStateHandle
import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.UserAccount
import com.citypulse.domain.repository.AuthError
import com.citypulse.domain.usecase.SignOut
import com.citypulse.ui.auth.AuthFormError
import com.citypulse.ui.auth.AuthMode
import com.citypulse.ui.auth.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AuthViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuth()

    private fun viewModel(signUp: Boolean = false) = AuthViewModel(auth, SavedStateHandle(mapOf("signUp" to signUp)))

    @Test
    fun `valida el formulario antes de llamar a Firebase`() {
        val vm = viewModel(signUp = true)
        vm.submit()
        assertEquals(AuthFormError.NameRequired, vm.uiState.value.error)
        vm.onNameChange("Ana")
        vm.onEmailChange("ana@correo")
        vm.submit()
        assertEquals(AuthFormError.InvalidEmail, vm.uiState.value.error)
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("123")
        vm.submit()
        assertEquals(AuthFormError.ShortPassword, vm.uiState.value.error)
        assertNull(auth.user.value)
    }

    @Test
    fun `al registrarse inicia sesion con el nombre elegido`() {
        val vm = viewModel(signUp = true)
        vm.onNameChange("Ana")
        vm.onEmailChange(" ana@correo.com ")
        vm.onPasswordChange("secreto1")
        vm.submit()
        assertTrue(vm.uiState.value.done)
        assertEquals("Ana", auth.user.value?.displayName)
    }

    @Test
    fun `traduce los errores de Firebase`() {
        auth.failWith = AuthError.InvalidCredentials
        val vm = viewModel()
        vm.onEmailChange("ana@correo.com")
        vm.onPasswordChange("secreto1")
        vm.submit()
        assertEquals(AuthFormError.InvalidCredentials, vm.uiState.value.error)
        assertFalse(vm.uiState.value.done)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `olvide mi contrasena envia el correo a la direccion escrita`() {
        val vm = viewModel()
        vm.resetPassword()
        assertEquals(AuthFormError.InvalidEmail, vm.uiState.value.error)
        vm.onEmailChange("ana@correo.com")
        vm.resetPassword()
        assertEquals(listOf("ana@correo.com"), auth.resetRequests)
        assertEquals("ana@correo.com", vm.uiState.value.resetSentTo)
        vm.setMode(AuthMode.SignUp)
        assertNull(vm.uiState.value.resetSentTo)
    }

    @Test
    fun `cerrar sesion quita los guardados del telefono`() = runTest {
        val saved = FakeSaved()
        saved.save(SampleData.events(LocalDate.of(2026, 10, 20)).first())
        auth.user.value = UserAccount("uid", "Ana", "ana@correo.com")
        SignOut(auth, saved).invoke()
        assertNull(auth.user.value)
        assertTrue(saved.observeSavedIds().first().isEmpty())
    }

    @Test
    fun `el nombre visible usa el correo si no hay nombre`() {
        val account = UserAccount("uid", null, "viajera@correo.com")
        assertEquals("viajera", account.displayName)
        assertEquals("V", account.initial)
    }
}
