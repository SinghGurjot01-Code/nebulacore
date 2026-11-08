package com.nebula.app.data.remote

import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseHelpersTest {

    @Test
    fun parsesUserWithMetadataName() {
        val json = JSONObject(
            """{"id":"u1","email":"sam@example.com","email_confirmed_at":"2026-11-08T10:00:00Z",
               "user_metadata":{"full_name":"Sam Player"}}"""
        )
        val user = parseUser(json)
        assertEquals("u1", user.id)
        assertEquals("Sam Player", user.fullName)
        assertTrue(user.emailVerified)
    }

    @Test
    fun unverifiedUserFallsBackToEmailName() {
        val json = JSONObject("""{"id":"u2","email":"sam@example.com","email_confirmed_at":null}""")
        val user = parseUser(json)
        assertEquals("sam", user.fullName)
        assertFalse(user.emailVerified)
    }

    @Test
    fun readsAuthAndPostgrestErrors() {
        val auth = SupabaseClient.Response(400, """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}""")
        assertEquals("invalid_credentials", auth.errorCode())
        assertEquals("Invalid login credentials", auth.errorMessage())

        val rpc = SupabaseClient.Response(400, """{"code":"P0001","message":"That time slot is full. Please pick another."}""")
        assertEquals("That time slot is full. Please pick another.", rpc.errorMessage())
        assertNull(rpc.errorCode())
    }

    @Test
    fun unparseableErrorUsesFallback() {
        val response = SupabaseClient.Response(502, "<html>bad gateway</html>")
        assertEquals("fallback", response.errorMessage("fallback"))
        assertFalse(response.ok)
        assertTrue(SupabaseClient.Response(204, "").ok)
    }

    @Test
    fun urlEncodingEscapesSpecialCharacters() {
        assertEquals("nebula%3A%2F%2Fauth%2Freset", enc("nebula://auth/reset"))
        assertEquals("a%26b", enc("a&b"))
    }

    @Test
    fun safeResultTurnsExceptionsIntoUserMessages() = runTest {
        val known = safeResult<Unit> { throw NebulaException("Slot is full.") }
        assertEquals("Slot is full.", known.exceptionOrNull()?.message)

        val unknown = safeResult<Unit> { throw IllegalStateException("internal detail") }
        assertEquals("Something went wrong. Please try again.", unknown.exceptionOrNull()?.message)

        assertEquals(5, safeResult { 5 }.getOrNull())
    }
}
