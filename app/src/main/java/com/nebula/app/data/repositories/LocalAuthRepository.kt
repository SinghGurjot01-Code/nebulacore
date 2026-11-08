package com.nebula.app.data.repositories

import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.AuthRepository
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Local demo authentication. Accounts live only on this device and are never
 * sent anywhere. Replace with a Supabase Auth implementation later.
 *
 * Built-in demo account: demo@nebula.app / Nebula123
 */
class LocalAuthRepository(
    private val store: KeyValueStore,
    private val simulatedLatencyMs: Long = 600L
) : AuthRepository {

    private data class Account(
        val email: String,
        val name: String,
        val passwordHash: String,
        val verified: Boolean
    ) {
        fun toUser() = User(id = email, fullName = name, email = email, emailVerified = verified)
    }

    private val seedAccount = Account(
        email = DEMO_EMAIL,
        name = "Alex Demo",
        passwordHash = hash(DEMO_PASSWORD),
        verified = true
    )

    override val googleSignInAvailable: Boolean = false

    override val session: Flow<User?> =
        combine(store.observe(KEY_SESSION), store.observe(KEY_ACCOUNTS)) { email, raw ->
            if (email == null) null else findAccount(parse(raw), email)?.toUser()
        }

    override suspend fun login(email: String, password: String): Result<User> {
        delay(simulatedLatencyMs)
        val accounts = parse(store.observe(KEY_ACCOUNTS).first())
        val account = findAccount(accounts, email)
        return if (account != null && account.passwordHash == hash(password)) {
            store.put(KEY_SESSION, account.email)
            Result.success(account.toUser())
        } else {
            Result.failure(NebulaException("Incorrect email or password."))
        }
    }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> {
        delay(simulatedLatencyMs)
        val accounts = parse(store.observe(KEY_ACCOUNTS).first())
        if (findAccount(accounts, email) != null) {
            return Result.failure(NebulaException("An account with this email already exists."))
        }
        val account = Account(
            email = email.trim().lowercase(),
            name = fullName.trim(),
            passwordHash = hash(password),
            verified = false
        )
        save(accounts + account)
        store.put(KEY_SESSION, account.email)
        return Result.success(account.toUser())
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        delay(simulatedLatencyMs)
        // Always succeeds so the screen never reveals whether an account exists.
        store.put(KEY_RESET_EMAIL, email.trim().lowercase())
        return Result.success(Unit)
    }

    override suspend fun resetPassword(newPassword: String): Result<Unit> {
        delay(simulatedLatencyMs)
        val email = store.observe(KEY_RESET_EMAIL).first()
            ?: return Result.failure(NebulaException("Start from Forgot password first."))
        val accounts = parse(store.observe(KEY_ACCOUNTS).first())
        val account = findAccount(accounts, email)
            ?: return Result.failure(NebulaException("No demo account matches that email on this device."))
        val updated = account.copy(passwordHash = hash(newPassword))
        save(accounts.filterNot { it.email == email } + updated)
        store.put(KEY_RESET_EMAIL, null)
        return Result.success(Unit)
    }

    override suspend fun markEmailVerified(): Result<Unit> {
        val email = store.observe(KEY_SESSION).first()
            ?: return Result.failure(NebulaException("You are not signed in."))
        return modifyAccount(email) { it.copy(verified = true) }
    }

    override suspend fun updateFullName(fullName: String): Result<Unit> {
        val email = store.observe(KEY_SESSION).first()
            ?: return Result.failure(NebulaException("You are not signed in."))
        return modifyAccount(email) { it.copy(name = fullName.trim()) }
    }

    override suspend fun logout() {
        store.put(KEY_SESSION, null)
    }

    private suspend fun modifyAccount(email: String, change: (Account) -> Account): Result<Unit> {
        val accounts = parse(store.observe(KEY_ACCOUNTS).first())
        val account = findAccount(accounts, email)
            ?: return Result.failure(NebulaException("Account not found."))
        save(accounts.filterNot { it.email == email } + change(account))
        return Result.success(Unit)
    }

    private fun findAccount(stored: List<Account>, email: String): Account? {
        val key = email.trim().lowercase()
        return stored.firstOrNull { it.email == key } ?: seedAccount.takeIf { it.email == key }
    }

    private suspend fun save(accounts: List<Account>) {
        val raw = accounts.joinToString("\n") {
            listOf(it.email, it.name.replace("\t", " ").replace("\n", " "), it.passwordHash, it.verified.toString())
                .joinToString("\t")
        }
        store.put(KEY_ACCOUNTS, raw)
    }

    private fun parse(raw: String?): List<Account> =
        raw.orEmpty().lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size == 4) Account(parts[0], parts[1], parts[2], parts[3] == "true") else null
        }

    companion object {
        const val DEMO_EMAIL = "demo@nebula.app"
        const val DEMO_PASSWORD = "Nebula123"
        private const val KEY_SESSION = "auth_session_email"
        private const val KEY_ACCOUNTS = "auth_accounts"
        private const val KEY_RESET_EMAIL = "auth_reset_email"

        private fun hash(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
