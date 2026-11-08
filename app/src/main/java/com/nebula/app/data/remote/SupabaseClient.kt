package com.nebula.app.data.remote

import android.net.Uri
import com.nebula.app.BuildConfig
import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.cancellation.CancellationException

/** Project URL and public (anon) key come from gradle.properties. Never put a service-role key here. */
object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()

    const val REDIRECT_RESET = "nebula://auth/reset"
    const val REDIRECT_VERIFIED = "nebula://auth/verified"
    const val REDIRECT_OAUTH = "nebula://auth/callback"
}

object SupabaseKeys {
    const val ACCESS = "sb_access_token"
    const val REFRESH = "sb_refresh_token"
    const val USER = "sb_user"
    const val PENDING_EMAIL = "sb_pending_email"
    const val RECOVERY = "sb_recovery"
    const val PKCE_VERIFIER = "sb_pkce_verifier"
}

/** PKCE (RFC 7636) helpers for the OAuth code flow. */
object Pkce {
    fun newVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return encode(bytes)
    }

    fun challenge(verifier: String): String =
        encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    private fun encode(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

internal fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)

fun parseUser(json: JSONObject): User {
    val email = json.optString("email")
    val meta = json.optJSONObject("user_metadata")
    val name = meta?.str("full_name")?.takeIf { it.isNotBlank() }
        ?: meta?.str("name")?.takeIf { it.isNotBlank() }
        ?: email.substringBefore("@")
    return User(
        id = json.optString("id"),
        fullName = name,
        email = email,
        emailVerified = !json.isNull("email_confirmed_at") && json.has("email_confirmed_at")
    )
}

/** Runs a block and turns any failure into a user-safe [Result]. */
suspend fun <T> safeResult(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: NebulaException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(NebulaException("Something went wrong. Please try again."))
    }

/**
 * Minimal Supabase REST client (Auth + PostgREST) built on HttpURLConnection so the app needs
 * no extra dependencies. Tokens are kept in the [KeyValueStore].
 */
class SupabaseClient(
    baseUrl: String,
    private val anonKey: String,
    private val store: KeyValueStore
) {
    private val base = baseUrl.trimEnd('/')

    data class Response(val code: Int, val body: String) {
        val ok: Boolean get() = code in 200..299

        fun json(): JSONObject = JSONObject(body)

        fun errorMessage(fallback: String = "Something went wrong. Please try again."): String {
            val parsed = try { JSONObject(body) } catch (e: Exception) { return fallback }
            val message = parsed.str("message") ?: parsed.str("msg") ?: parsed.str("error_description")
            return message?.takeIf { it.isNotBlank() } ?: fallback
        }

        fun errorCode(): String? =
            try { JSONObject(body).str("error_code") } catch (e: Exception) { null }
    }

    suspend fun accessToken(): String? = store.observe(SupabaseKeys.ACCESS).first()

    suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        authenticated: Boolean = true
    ): Response {
        val token = if (authenticated) accessToken() else null
        var response = execute(method, path, body, headers, token)
        if (response.code == 401 && token != null && refreshSession()) {
            response = execute(method, path, body, headers, accessToken())
        }
        return response
    }

    private suspend fun execute(
        method: String,
        path: String,
        body: String?,
        headers: Map<String, String>,
        bearer: String?
    ): Response = withContext(Dispatchers.IO) {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Authorization", "Bearer " + (bearer ?: anonKey))
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Response(code, text)
        } catch (e: IOException) {
            throw NebulaException("Can't reach Nebula. Check your internet connection and try again.")
        } finally {
            connection.disconnect()
        }
    }

    /** Saves tokens and the user from an auth response containing access_token and user. */
    suspend fun storeSession(json: JSONObject): User {
        val userJson = json.getJSONObject("user")
        store.put(SupabaseKeys.ACCESS, json.getString("access_token"))
        store.put(SupabaseKeys.REFRESH, json.optString("refresh_token"))
        store.put(SupabaseKeys.PENDING_EMAIL, null)
        store.put(SupabaseKeys.RECOVERY, null)
        store.put(SupabaseKeys.USER, userJson.toString())
        return parseUser(userJson)
    }

    suspend fun clearSession() {
        store.put(SupabaseKeys.ACCESS, null)
        store.put(SupabaseKeys.REFRESH, null)
        store.put(SupabaseKeys.RECOVERY, null)
        store.put(SupabaseKeys.USER, null)
    }

    private suspend fun refreshSession(): Boolean {
        val refresh = store.observe(SupabaseKeys.REFRESH).first() ?: return false
        return try {
            val response = execute(
                "POST", "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh).toString(), emptyMap(), null
            )
            if (response.ok) {
                storeSession(response.json())
                true
            } else {
                clearSession()
                false
            }
        } catch (e: NebulaException) {
            false
        }
    }

    /** URL that starts Google sign-in in the browser; Supabase redirects back to nebula://auth/callback?code=... */
    fun googleAuthorizeUrl(codeChallenge: String): String =
        base + "/auth/v1/authorize?provider=google" +
            "&redirect_to=" + enc(SupabaseConfig.REDIRECT_OAUTH) +
            "&code_challenge=" + codeChallenge +
            "&code_challenge_method=s256"

    /** Parses the query string of a nebula://auth/... link (used by the OAuth code flow). */
    fun parseQueryParams(uri: String): Map<String, String> {
        val query = Uri.parse(uri).encodedQuery ?: return emptyMap()
        return query.split("&").mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else Uri.decode(part.substring(0, index)) to Uri.decode(part.substring(index + 1).replace('+', ' '))
        }.toMap()
    }

    /** Parses tokens from a nebula://auth/... link (values arrive in the URL fragment). */
    fun parseLinkParams(uri: String): Map<String, String> {
        val fragment = Uri.parse(uri).fragment ?: return emptyMap()
        return fragment.split("&").mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else Uri.decode(part.substring(0, index)) to Uri.decode(part.substring(index + 1))
        }.toMap()
    }
}
