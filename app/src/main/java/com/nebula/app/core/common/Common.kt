package com.nebula.app.core.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nebula.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted

/** Loading / success / error wrapper used by screens that fetch data. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

/** True when the app runs on local demo data instead of Supabase. */
val LocalDemoMode = staticCompositionLocalOf { true }

suspend fun <T> loadCatching(block: suspend () -> T): Loadable<T> =
    try {
        Loadable.Ready(block())
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: Exception) {
        Loadable.Failed(e.userMessage())
    }

val UiSharingStarted = SharingStarted.WhileSubscribed(5_000)

/** Creates a ViewModel with access to the app's repositories. */
@Composable
inline fun <reified VM : ViewModel> nebulaViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM
): VM {
    val container = LocalAppContainer.current
    return viewModel<VM>(
        key = key,
        factory = viewModelFactory {
            initializer<VM> { create(container) }
        }
    )
}

fun Throwable.userMessage(fallback: String = "Something went wrong. Please try again."): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
