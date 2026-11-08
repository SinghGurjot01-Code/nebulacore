package com.nebula.app.domain.models

enum class CheckStatus { Passed, Failed, Pending }

data class PreflightCheck(
    val id: String,
    val title: String,
    val detail: String,
    val status: CheckStatus
)

/**
 * Credentials a real streaming host would provide. This type only defines the
 * integration boundary: nothing in the app constructs one in this phase.
 */
data class StreamSession(
    val gameId: String,
    val hostAddress: String,
    val sessionToken: String
)

sealed interface StreamingState {
    data object Idle : StreamingState
    data class Preparing(val checks: List<PreflightCheck>) : StreamingState
    data class Unavailable(val checks: List<PreflightCheck>, val reason: String) : StreamingState
    data class Streaming(val session: StreamSession) : StreamingState
    data class Disconnected(val reason: String) : StreamingState
}
