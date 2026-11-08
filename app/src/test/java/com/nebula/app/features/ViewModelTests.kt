package com.nebula.app.features

import com.nebula.app.data.local.InMemoryKeyValueStore
import com.nebula.app.data.repositories.LocalAuthRepository
import com.nebula.app.data.repositories.LocalSupportRepository
import com.nebula.app.features.auth.LoginViewModel
import com.nebula.app.features.auth.RegisterViewModel
import com.nebula.app.features.support.CreateTicketViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTests {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun auth() = LocalAuthRepository(InMemoryKeyValueStore(), simulatedLatencyMs = 0)

    @Test
    fun loginShowsFieldErrorsAndDoesNotSubmit() = runTest(dispatcher) {
        val vm = LoginViewModel(auth())
        var done = false
        vm.submit { done = true }
        advanceUntilIdle()
        val state = vm.state.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertFalse(state.loading)
        assertFalse(done)
    }

    @Test
    fun loginWithDemoAccountSucceeds() = runTest(dispatcher) {
        val vm = LoginViewModel(auth())
        vm.useDemoAccount()
        var done = false
        vm.submit { done = true }
        advanceUntilIdle()
        assertTrue(done)
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun loginWithWrongPasswordShowsFormError() = runTest(dispatcher) {
        val vm = LoginViewModel(auth())
        vm.onEmailChange(LocalAuthRepository.DEMO_EMAIL)
        vm.onPasswordChange("wrong-pass1")
        var done = false
        vm.submit { done = true }
        advanceUntilIdle()
        assertFalse(done)
        assertEquals("Incorrect email or password.", vm.state.value.formError)
    }

    @Test
    fun registerValidatesEveryField() = runTest(dispatcher) {
        val vm = RegisterViewModel(auth())
        vm.onNameChange("A")
        vm.onEmailChange("bad")
        vm.onPasswordChange("short")
        vm.onConfirmChange("different")
        var done = false
        vm.submit { done = true }
        advanceUntilIdle()
        val s = vm.state.value
        assertNotNull(s.nameError)
        assertNotNull(s.emailError)
        assertNotNull(s.passwordError)
        assertNotNull(s.confirmError)
        assertFalse(done)
    }

    @Test
    fun registerSucceedsWithValidInput() = runTest(dispatcher) {
        val vm = RegisterViewModel(auth())
        vm.onNameChange("Sam Player")
        vm.onEmailChange("sam@example.com")
        vm.onPasswordChange("Nebula123")
        vm.onConfirmChange("Nebula123")
        var done = false
        vm.submit { done = true }
        advanceUntilIdle()
        assertTrue(done)
    }

    @Test
    fun createTicketValidatesThenCreates() = runTest(dispatcher) {
        val vm = CreateTicketViewModel(LocalSupportRepository(simulatedLatencyMs = 0))
        var createdId: String? = null
        vm.submit { createdId = it }
        advanceUntilIdle()
        assertNotNull(vm.state.value.subjectError)
        assertNotNull(vm.state.value.descriptionError)
        assertNull(createdId)

        vm.onSubject("Slow artwork")
        vm.onDescription("Game covers load slowly on mobile data.")
        vm.submit { createdId = it }
        advanceUntilIdle()
        assertNotNull(createdId)
        assertFalse(vm.state.value.loading)
    }
}
