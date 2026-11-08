package com.nebula.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.data.repositories.LocalAuthRepository
import com.nebula.app.domain.Validation
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ Login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, formError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun useDemoAccount() = _state.update {
        it.copy(
            email = LocalAuthRepository.DEMO_EMAIL,
            password = LocalAuthRepository.DEMO_PASSWORD,
            emailError = null, passwordError = null, formError = null
        )
    }

    fun submit(onDone: () -> Unit) {
        val current = _state.value
        if (current.loading) return
        val emailError = Validation.email(current.email)
        val passwordError = Validation.loginPassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, formError = null) }
            auth.login(current.email, current.password)
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                    onDone()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.userMessage()) }
                }
        }
    }
}

// ------------------------------------------------------------------ Register

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false
)

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(fullName = value, nameError = null, formError = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, formError = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, formError = null) }
    fun onConfirmChange(value: String) = _state.update { it.copy(confirmPassword = value, confirmError = null, formError = null) }

    fun submit(onDone: () -> Unit) {
        val s = _state.value
        if (s.loading) return
        val nameError = Validation.fullName(s.fullName)
        val emailError = Validation.email(s.email)
        val passwordError = Validation.newPassword(s.password)
        val confirmError = Validation.confirmPassword(s.password, s.confirmPassword)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) {
            _state.update {
                it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError, confirmError = confirmError)
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, formError = null) }
            auth.register(s.fullName, s.email, s.password)
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                    onDone()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.userMessage()) }
                }
        }
    }
}

// ------------------------------------------------------------------ Forgot / reset

data class ForgotUiState(
    val email: String = "",
    val emailError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val requested: Boolean = false
)

class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotUiState())
    val state: StateFlow<ForgotUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = Validation.email(s.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, formError = null) }
            auth.requestPasswordReset(s.email)
                .onSuccess { _state.update { it.copy(loading = false, requested = true) } }
                .onFailure { error -> _state.update { it.copy(loading = false, formError = error.userMessage()) } }
        }
    }
}

data class ResetUiState(
    val linkReady: Boolean = true,
    val linkError: String? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val completed: Boolean = false
)

class ResetPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    // Real password reset needs the emailed link; the demo account flow does not.
    private val _state = MutableStateFlow(ResetUiState(linkReady = auth.isDemo))
    val state: StateFlow<ResetUiState> = _state.asStateFlow()

    fun onLink(uri: String?) {
        if (uri == null || _state.value.linkReady) return
        viewModelScope.launch {
            if (auth.handleAuthLink(uri)) {
                _state.update { it.copy(linkReady = true, linkError = null) }
            } else {
                _state.update { it.copy(linkError = "This reset link is invalid or has expired. Request a new one.") }
            }
        }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, formError = null) }
    fun onConfirmChange(value: String) = _state.update { it.copy(confirmPassword = value, confirmError = null, formError = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val passwordError = Validation.newPassword(s.password)
        val confirmError = Validation.confirmPassword(s.password, s.confirmPassword)
        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, formError = null) }
            auth.resetPassword(s.password)
                .onSuccess { _state.update { it.copy(loading = false, completed = true) } }
                .onFailure { error -> _state.update { it.copy(loading = false, formError = error.userMessage()) } }
        }
    }
}

// ------------------------------------------------------------------ Verify / splash

class VerifyEmailViewModel(private val auth: AuthRepository) : ViewModel() {
    val isDemo: Boolean = auth.isDemo
    val user: StateFlow<User?> = auth.session.stateIn(viewModelScope, UiSharingStarted, null)
    val pendingEmail: StateFlow<String?> =
        auth.pendingVerificationEmail.stateIn(viewModelScope, UiSharingStarted, null)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun markVerified() {
        viewModelScope.launch {
            _loading.value = true
            _message.value = auth.markEmailVerified().exceptionOrNull()?.userMessage()
            _loading.value = false
        }
    }

    fun resend() {
        viewModelScope.launch {
            _loading.value = true
            _message.value = auth.resendVerification().fold(
                onSuccess = { "Verification email sent." },
                onFailure = { it.userMessage() }
            )
            _loading.value = false
        }
    }

    fun onLink(uri: String?) {
        if (uri == null) return
        viewModelScope.launch {
            if (!auth.handleAuthLink(uri)) _message.value = "This verification link is invalid or has expired."
        }
    }
}

class SplashViewModel(private val auth: AuthRepository) : ViewModel() {
    suspend fun isSignedIn(): Boolean = auth.session.first() != null
}

// ------------------------------------------------------------------ Google sign-in

data class GoogleUiState(val loading: Boolean = false, val error: String? = null)

class GoogleSignInViewModel(private val auth: AuthRepository) : ViewModel() {
    val available: Boolean = auth.googleSignInAvailable

    private val _state = MutableStateFlow(GoogleUiState())
    val state: StateFlow<GoogleUiState> = _state.asStateFlow()

    /** Prepares PKCE and hands the authorize URL to [open] (which launches the browser). */
    fun start(open: (String) -> Unit) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { GoogleUiState(loading = true) }
            auth.startGoogleSignIn()
                .onSuccess { url ->
                    _state.update { GoogleUiState() }
                    open(url)
                }
                .onFailure { error -> _state.update { GoogleUiState(error = error.userMessage()) } }
        }
    }
}

data class OAuthCallbackUiState(val error: String? = null)

class OAuthCallbackViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(OAuthCallbackUiState())
    val state: StateFlow<OAuthCallbackUiState> = _state.asStateFlow()
    private var handled = false

    fun handle(uri: String?, onSuccess: () -> Unit) {
        if (handled) return
        handled = true
        viewModelScope.launch {
            if (uri == null) {
                _state.update { OAuthCallbackUiState("This sign-in link is not valid.") }
                return@launch
            }
            auth.completeGoogleSignIn(uri)
                .onSuccess { onSuccess() }
                .onFailure { error -> _state.update { OAuthCallbackUiState(error.userMessage()) } }
        }
    }
}
