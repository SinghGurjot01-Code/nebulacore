package com.nebula.app.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaMark
import com.nebula.app.design.components.NebulaTextField
import com.nebula.app.design.components.NebulaTopBar
import com.nebula.app.design.components.NebulaWordmark
import com.nebula.app.design.theme.NebulaColors
import kotlinx.coroutines.delay

@Composable
private fun AuthPage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        NebulaTopBar(title = "", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = NebulaColors.Secondary)
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ Splash

@Composable
fun SplashScreen(onSignedIn: () -> Unit, onSignedOut: () -> Unit) {
    val viewModel = nebulaViewModel { SplashViewModel(it.authRepository) }
    LaunchedEffect(Unit) {
        val signedIn = viewModel.isSignedIn()
        delay(900)
        if (signedIn) onSignedIn() else onSignedOut()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NebulaColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NebulaMark(size = 72.dp)
            Text("Nebula", style = MaterialTheme.typography.headlineMedium)
            Text("Cloud gaming, in development", style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
        }
    }
}

// ------------------------------------------------------------------ Welcome

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    val demo = LocalDemoMode.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NebulaColors.Surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        NebulaWordmark(modifier = Modifier.padding(top = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NebulaMark(size = 64.dp)
            Text("Your games, streamed from the cloud.", style = MaterialTheme.typography.displaySmall)
            Text(
                "Browse the catalogue, plan a session and get ready to play on any device.",
                style = MaterialTheme.typography.bodyLarge,
                color = NebulaColors.Secondary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NebulaButton("Create account", onRegister, Modifier.fillMaxWidth())
            NebulaButton("Log in", onLogin, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
            Text(
                if (demo) "Nebula is in development. Accounts, bookings and tickets in this build are local demos stored on your device."
                else "Nebula is in development. Cloud streaming is not available yet.",
                style = MaterialTheme.typography.bodySmall,
                color = NebulaColors.Secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ Google

/** Opens Google sign-in in a Custom Tab; the result returns through nebula://auth/callback. */
@Composable
private fun GoogleSignInButton() {
    val viewModel = nebulaViewModel(key = "google") { GoogleSignInViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    NebulaButton(
        text = "Continue with Google",
        onClick = {
            viewModel.start { url ->
                try {
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        kind = ButtonKind.Secondary,
        enabled = viewModel.available,
        loading = state.loading
    )
    if (!viewModel.available) {
        Text(
            "Google sign-in needs the Nebula backend and is not available in demo mode.",
            style = MaterialTheme.typography.bodySmall,
            color = NebulaColors.Secondary
        )
    }
    state.error?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
}

/** Landing screen for nebula://auth/callback: finishes the Google sign-in, then opens Home. */
@Composable
fun OAuthCallbackScreen(linkUri: String?, onSignedIn: () -> Unit, onBackToLogin: () -> Unit) {
    val viewModel = nebulaViewModel { OAuthCallbackViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(linkUri) { viewModel.handle(linkUri, onSignedIn) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NebulaColors.Surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val error = state.error
        if (error == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = NebulaColors.Accent)
                Text("Signing you in…", style = MaterialTheme.typography.bodyLarge, color = NebulaColors.Secondary)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBanner(error, Icons.Filled.Info, tone = ChipTone.Danger)
                NebulaButton("Back to log in", onBackToLogin, Modifier.fillMaxWidth())
            }
        }
    }
}

// ------------------------------------------------------------------ Login

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit
) {
    val viewModel = nebulaViewModel { LoginViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val submit = { viewModel.submit(onLoggedIn) }
    val demo = LocalDemoMode.current

    AuthPage("Welcome back", "Log in to continue to Nebula.", onBack) {
        NebulaTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            error = state.emailError,
            enabled = !state.loading
        )
        NebulaTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            error = state.passwordError,
            enabled = !state.loading,
            onImeAction = { submit() }
        )
        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", color = NebulaColors.Accent, fontWeight = FontWeight.SemiBold)
        }
        state.formError?.let {
            InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger)
        }
        NebulaButton(
            text = "Log in",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            loading = state.loading
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NebulaColors.Border)
            Text("or", color = NebulaColors.Secondary, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f), color = NebulaColors.Border)
        }
        GoogleSignInButton()

        if (demo) {
            InfoBanner(
                text = "Demo account: demo@nebula.app with password Nebula123. Tap below to fill it in.",
                icon = Icons.Filled.Person
            )
            NebulaButton(
                text = "Use demo account",
                onClick = viewModel::useDemoAccount,
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Secondary,
                enabled = !state.loading
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New to Nebula?", color = NebulaColors.Secondary)
            TextButton(onClick = onRegister) {
                Text("Create account", color = NebulaColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ------------------------------------------------------------------ Register

@Composable
fun RegisterScreen(onBack: () -> Unit, onRegistered: () -> Unit, onLogin: () -> Unit) {
    val viewModel = nebulaViewModel { RegisterViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val submit = { viewModel.submit(onRegistered) }
    val demo = LocalDemoMode.current

    AuthPage(
        "Create your account",
        if (demo) "A local demo account is created on this device only." else "We will email you a link to confirm your address.",
        onBack
    ) {
        NebulaTextField(
            value = state.fullName,
            onValueChange = viewModel::onNameChange,
            label = "Full name",
            leadingIcon = Icons.Filled.Person,
            error = state.nameError,
            enabled = !state.loading
        )
        NebulaTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            leadingIcon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            error = state.emailError,
            enabled = !state.loading
        )
        NebulaTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            error = state.passwordError,
            supportingText = "At least 8 characters with a letter and a number.",
            enabled = !state.loading
        )
        NebulaTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmChange,
            label = "Confirm password",
            leadingIcon = Icons.Filled.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            error = state.confirmError,
            enabled = !state.loading,
            onImeAction = { submit() }
        )
        state.formError?.let {
            InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger)
        }
        NebulaButton(
            text = "Create account",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            loading = state.loading
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NebulaColors.Border)
            Text("or", color = NebulaColors.Secondary, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f), color = NebulaColors.Border)
        }
        GoogleSignInButton()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = NebulaColors.Secondary)
            TextButton(onClick = onLogin) {
                Text("Log in", color = NebulaColors.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ------------------------------------------------------------------ Forgot password

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onContinueToReset: () -> Unit) {
    val viewModel = nebulaViewModel { ForgotPasswordViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthPage("Forgot password", "Enter your email and we will start a password reset.", onBack) {
        if (state.requested) {
            InfoBanner(
                text = "Demo mode: no email is sent. If an account exists on this device you can set a new password on the next screen.",
                icon = Icons.Filled.CheckCircle,
                tone = ChipTone.Success
            )
            NebulaButton("Set a new password", onContinueToReset, Modifier.fillMaxWidth())
        } else {
            NebulaTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                error = state.emailError,
                enabled = !state.loading,
                onImeAction = viewModel::submit
            )
            state.formError?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
            NebulaButton(
                text = "Send reset link",
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading
            )
        }
    }
}

// ------------------------------------------------------------------ Reset password

@Composable
fun ResetPasswordScreen(onBack: () -> Unit, onDone: () -> Unit, linkUri: String? = null) {
    val viewModel = nebulaViewModel { ResetPasswordViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(linkUri) { viewModel.onLink(linkUri) }

    AuthPage("Set a new password", "Choose a password you have not used before.", onBack) {
        if (state.completed) {
            InfoBanner(
                text = "Your password was updated. You can log in now.",
                icon = Icons.Filled.CheckCircle,
                tone = ChipTone.Success
            )
            NebulaButton("Back to log in", onDone, Modifier.fillMaxWidth())
        } else if (!state.linkReady) {
            InfoBanner(
                text = state.linkError
                    ?: "Open the reset link from your email on this phone to choose a new password.",
                icon = Icons.Filled.Info,
                tone = if (state.linkError != null) ChipTone.Danger else ChipTone.Accent
            )
            NebulaButton("Back", onBack, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
        } else {
            NebulaTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "New password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                error = state.passwordError,
                supportingText = "At least 8 characters with a letter and a number.",
                enabled = !state.loading
            )
            NebulaTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = "Confirm new password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                error = state.confirmError,
                enabled = !state.loading,
                onImeAction = viewModel::submit
            )
            state.formError?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
            NebulaButton(
                text = "Update password",
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading
            )
        }
    }
}

// ------------------------------------------------------------------ Verify email

@Composable
fun VerifyEmailScreen(onContinue: () -> Unit, onLogin: () -> Unit, linkUri: String? = null) {
    val viewModel = nebulaViewModel { VerifyEmailViewModel(it.authRepository) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val pendingEmail by viewModel.pendingEmail.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(linkUri) { viewModel.onLink(linkUri) }
    val verified = user?.emailVerified == true
    val email = user?.email ?: pendingEmail

    AuthPage(
        title = if (verified) "Email verified" else "Verify your email",
        subtitle = when {
            verified -> "Thanks, your account is ready."
            viewModel.isDemo -> "Real verification emails need the Nebula backend, which is not connected in demo mode."
            else -> "Open the link in the email we sent to finish creating your account."
        },
        onBack = null
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (verified) NebulaColors.SuccessSoft else NebulaColors.AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (verified) Icons.Filled.CheckCircle else Icons.Filled.Email,
                contentDescription = null,
                tint = if (verified) NebulaColors.Success else NebulaColors.Accent,
                modifier = Modifier.size(34.dp)
            )
        }
        email?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        message?.let { InfoBanner(it, Icons.Filled.Info) }

        if (!verified) {
            if (viewModel.isDemo) {
                InfoBanner(
                    text = "Demo mode: no email was sent. Use the button below to mark this demo account as verified.",
                    icon = Icons.Filled.Info
                )
                NebulaButton(
                    text = "Mark email as verified (demo)",
                    onClick = viewModel::markVerified,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Accent,
                    loading = loading
                )
            } else {
                NebulaButton(
                    text = "I have verified my email",
                    onClick = viewModel::markVerified,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Accent,
                    loading = loading
                )
                NebulaButton(
                    text = "Resend email",
                    onClick = viewModel::resend,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary,
                    enabled = !loading
                )
            }
        }
        when {
            verified -> NebulaButton("Continue to Nebula", onContinue, Modifier.fillMaxWidth(), kind = ButtonKind.Primary)
            user == null && !viewModel.isDemo ->
                NebulaButton("Back to log in", onLogin, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
            else -> NebulaButton("Skip for now", onContinue, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
        }
    }
}
