package com.citypulse.ui

import com.citypulse.data.fake.SampleData
import com.citypulse.domain.model.UserAccount
import com.citypulse.domain.repository.AuthError
import com.citypulse.domain.repository.AuthException
import com.citypulse.domain.usecase.DeleteAccount
import com.citypulse.domain.usecase.SignOut
import com.citypulse.ui.auth.AuthFormError
import com.citypulse.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ProfileViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val auth = FakeAuth().apply { user.value = UserAccount("uid", "Ana", "ana@correo.com") }
    private val saved = FakeSaved()

    private fun viewModel() = ProfileViewModel(auth, saved, SignOut(auth, saved), DeleteAccount(auth, saved))

    @Test
    fun `eliminar la cuenta borra la nube, la cuenta y el telefono`() = runTest {
        saved.save(SampleData.events(LocalDate.of(2026, 10, 20)).first())
        val vm = viewModel()
        vm.deleteAccount("secreto1")
        assertEquals(listOf("secreto1"), auth.reauthPasswords)
        assertTrue(saved.cloudCleared)
        assertTrue(auth.deleted)
        assertNull(auth.user.value)
        assertTrue(saved.observeSavedIds().first().isEmpty())
        assertTrue(vm.deleteState.value.done)
    }

    @Test
    fun `con la contrasena incorrecta no se borra nada`() = runTest {
        saved.save(SampleData.events(LocalDate.of(2026, 10, 20)).first())
        auth.failWith = AuthError.InvalidCredentials
        val vm = viewModel()
        vm.deleteAccount("mala")
        assertEquals(AuthFormError.InvalidCredentials, vm.deleteState.value.error)
        assertFalse(vm.deleteState.value.running)
        assertFalse(saved.cloudCleared)
        assertFalse(auth.deleted)
        assertEquals(1, saved.observeSavedIds().first().size)
    }

    @Test
    fun `sin conexion la cuenta no se borra y avisa`() {
        saved.failCloud = true
        val vm = viewModel()
        vm.deleteAccount("secreto1")
        assertEquals(AuthFormError.Network, vm.deleteState.value.error)
        assertFalse(auth.deleted)
        assertNotNull(auth.user.value)
    }

    @Test
    fun `contrasena vacia no llama a Firebase`() {
        val vm = viewModel()
        vm.deleteAccount("")
        assertEquals(AuthFormError.InvalidCredentials, vm.deleteState.value.error)
        assertTrue(auth.reauthPasswords.isEmpty())
        vm.resetDelete()
        assertNull(vm.deleteState.value.error)
    }

    @Test
    fun `el caso de uso propaga el error de Firebase`() = runTest {
        auth.failWith = AuthError.TooManyRequests
        val error = runCatching { DeleteAccount(auth, saved).invoke("secreto1") }.exceptionOrNull()
        assertEquals(AuthError.TooManyRequests, (error as AuthException).error)
    }
}
