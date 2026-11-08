package com.nebula.app.data

import com.nebula.app.data.local.InMemoryKeyValueStore
import com.nebula.app.data.repositories.LocalAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAuthRepositoryTest {

    private fun repository() = LocalAuthRepository(InMemoryKeyValueStore(), simulatedLatencyMs = 0)

    @Test
    fun builtInDemoAccountCanLogIn() = runTest {
        val repo = repository()
        val result = repo.login(LocalAuthRepository.DEMO_EMAIL, LocalAuthRepository.DEMO_PASSWORD)
        assertTrue(result.isSuccess)
        assertEquals(LocalAuthRepository.DEMO_EMAIL, repo.session.first()?.email)
    }

    @Test
    fun wrongPasswordFails() = runTest {
        val repo = repository()
        val result = repo.login(LocalAuthRepository.DEMO_EMAIL, "wrong-password1")
        assertTrue(result.isFailure)
        assertNull(repo.session.first())
    }

    @Test
    fun registerCreatesSessionAndRejectsDuplicates() = runTest {
        val repo = repository()
        val created = repo.register("Sam Player", "Sam@Example.com", "Nebula123")
        assertTrue(created.isSuccess)
        val user = repo.session.first()
        assertNotNull(user)
        assertEquals("sam@example.com", user?.email)
        assertFalse(user?.emailVerified ?: true)

        assertTrue(repo.register("Other", "sam@example.com", "Nebula123").isFailure)
    }

    @Test
    fun logoutClearsSessionButKeepsAccount() = runTest {
        val repo = repository()
        repo.register("Sam Player", "sam@example.com", "Nebula123")
        repo.logout()
        assertNull(repo.session.first())
        assertTrue(repo.login("sam@example.com", "Nebula123").isSuccess)
    }

    @Test
    fun passwordResetFlowChangesPassword() = runTest {
        val repo = repository()
        repo.register("Sam Player", "sam@example.com", "Nebula123")
        repo.logout()
        repo.requestPasswordReset("sam@example.com")
        assertTrue(repo.resetPassword("Brandnew456").isSuccess)
        assertTrue(repo.login("sam@example.com", "Nebula123").isFailure)
        assertTrue(repo.login("sam@example.com", "Brandnew456").isSuccess)
    }

    @Test
    fun resetWithoutRequestFails() = runTest {
        assertTrue(repository().resetPassword("Brandnew456").isFailure)
    }

    @Test
    fun verifyAndRenameUpdateTheSessionUser() = runTest {
        val repo = repository()
        repo.register("Sam Player", "sam@example.com", "Nebula123")
        assertTrue(repo.markEmailVerified().isSuccess)
        assertTrue(repo.updateFullName("Samantha Player").isSuccess)
        val user = repo.session.first()
        assertEquals(true, user?.emailVerified)
        assertEquals("Samantha Player", user?.fullName)
    }

    @Test
    fun googleSignInIsNotAvailableInDemo() {
        assertFalse(repository().googleSignInAvailable)
    }
}
