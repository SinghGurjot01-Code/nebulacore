package com.nebula.app.data.remote

import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.AuthRepository
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** Supabase Auth (email and password). Google sign-in is not configured yet. */
class RemoteAuthRepository(
    private val client: SupabaseClient,
    private val store: KeyValueStore
) : AuthRepository {

    override val isDemo: Boolean = false
    override val googleSignInAvailable: Boolean = false

    override val session: Flow<User?> =
        store.observe(SupabaseKeys.USER).map { raw ->
            raw?.let { value -> try { parseUser(JSONObject(value)) } catch (e: Exception) { null } }
        }

    override val pendingVerificationEmail: Flow<String?> = store.observe(SupabaseKeys.PENDING_EMAIL)

    override suspend fun login(email: String, password: String): Result<User> = safeResult {
        val response = client.request(
            "POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password).toString(),
            authenticated = false
        )
        if (!response.ok) throw NebulaException(authMessage(response))
        client.storeSession(response.json())
    }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> = safeResult {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("data", JSONObject().put("full_name", fullName.trim()))
            .toString()
        val response = client.request(
            "POST", "/auth/v1/signup?redirect_to=" + enc(SupabaseConfig.REDIRECT_VERIFIED),
            body, authenticated = false
        )
        if (!response.ok) throw NebulaException(authMessage(response))
        val json = response.json()
        if (json.has("access_token")) {
            client.storeSession(json)
        } else {
            // Email confirmation is on: no session until the user opens the link we emailed.
            store.put(SupabaseKeys.PENDING_EMAIL, email.trim())
            User(json.optString("id"), fullName.trim(), email.trim(), emailVerified = false)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = safeResult {
        val response = client.request(
            "POST", "/auth/v1/recover?redirect_to=" + enc(SupabaseConfig.REDIRECT_RESET),
            JSONObject().put("email", email.trim()).toString(),
            authenticated = false
        )
        if (!response.ok) throw NebulaException(authMessage(response))
    }

    override suspend fun resetPassword(newPassword: String): Result<Unit> = safeResult {
        if (store.observe(SupabaseKeys.RECOVERY).first() != "true") {
            throw NebulaException("Open the reset link from your email on this phone first.")
        }
        val response = client.request(
            "PUT", "/auth/v1/user",
            JSONObject().put("password", newPassword).toString()
        )
        if (!response.ok) throw NebulaException(authMessage(response))
        client.clearSession()
    }

    override suspend fun markEmailVerified(): Result<Unit> = safeResult {
        val response = client.request("GET", "/auth/v1/user")
        if (!response.ok) throw NebulaException("Log in first, then check again.")
        val json = response.json()
        store.put(SupabaseKeys.USER, json.toString())
        if (!parseUser(json).emailVerified) {
            throw NebulaException("Your email is not verified yet. Open the link we sent you.")
        }
    }

    override suspend fun resendVerification(): Result<Unit> = safeResult {
        val email = store.observe(SupabaseKeys.PENDING_EMAIL).first()
            ?: store.observe(SupabaseKeys.USER).first()?.let { parseUser(JSONObject(it)).email }
            ?: throw NebulaException("There is no email to verify.")
        val response = client.request(
            "POST", "/auth/v1/resend?redirect_to=" + enc(SupabaseConfig.REDIRECT_VERIFIED),
            JSONObject().put("type", "signup").put("email", email).toString(),
            authenticated = false
        )
        if (!response.ok) throw NebulaException(authMessage(response))
    }

    override suspend fun handleAuthLink(uri: String): Boolean {
        return try {
            val params = client.parseLinkParams(uri)
            val access = params["access_token"] ?: return false
            store.put(SupabaseKeys.ACCESS, access)
            store.put(SupabaseKeys.REFRESH, params["refresh_token"])
            if (params["type"] == "recovery") {
                store.put(SupabaseKeys.RECOVERY, "true")
                true
            } else {
                val response = client.request("GET", "/auth/v1/user")
                if (!response.ok) return false
                store.put(SupabaseKeys.PENDING_EMAIL, null)
                store.put(SupabaseKeys.USER, response.json().toString())
                true
            }
        } catch (e: NebulaException) {
            false
        }
    }

    override suspend fun updateFullName(fullName: String): Result<Unit> = safeResult {
        val response = client.request(
            "PUT", "/auth/v1/user",
            JSONObject().put("data", JSONObject().put("full_name", fullName.trim())).toString()
        )
        if (!response.ok) throw NebulaException(authMessage(response))
        store.put(SupabaseKeys.USER, response.json().toString())
    }

    override suspend fun logout() {
        try {
            client.request("POST", "/auth/v1/logout")
        } catch (e: NebulaException) {
            // Offline logout still clears the local session.
        }
        client.clearSession()
    }

    private fun authMessage(response: SupabaseClient.Response): String = when (response.errorCode()) {
        "invalid_credentials" -> "Incorrect email or password."
        "email_not_confirmed" -> "Your email is not verified yet. Open the link we sent you."
        "user_already_exists", "email_exists" -> "An account with this email already exists."
        "over_request_rate_limit", "over_email_send_rate_limit" -> "Too many attempts. Please wait a minute and try again."
        else -> response.errorMessage()
    }
}
