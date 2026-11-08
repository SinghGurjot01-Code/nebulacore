package com.nebula.app.features.streaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.domain.models.CheckStatus
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.StreamingState
import com.nebula.app.domain.repositories.GameRepository
import com.nebula.app.domain.repositories.StreamingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StreamingUiState(
    val loading: Boolean = true,
    val game: Game? = null,
    val phase: StreamingState = StreamingState.Idle
)

class StreamingViewModel(
    private val games: GameRepository,
    private val streaming: StreamingRepository,
    private val gameId: String
) : ViewModel() {

    private val local = MutableStateFlow(StreamingUiState())

    /** Local state, overridden by the streaming client once a real one reports Streaming or Disconnected. */
    val state: StateFlow<StreamingUiState> =
        combine(local, streaming.client.state) { ui, client ->
            when (client) {
                is StreamingState.Streaming, is StreamingState.Disconnected -> ui.copy(phase = client)
                else -> ui
            }
        }.stateIn(viewModelScope, UiSharingStarted, StreamingUiState())

    init {
        viewModelScope.launch {
            val game = games.getGame(gameId)
            local.update { it.copy(loading = false, game = game) }
        }
    }

    fun prepare() {
        val game = local.value.game ?: return
        if (local.value.phase is StreamingState.Preparing) return
        viewModelScope.launch {
            local.update { it.copy(phase = StreamingState.Preparing(emptyList())) }
            delay(600)
            val checks = streaming.runPreflight(game)
            val offline = checks.any { it.id == "network" && it.status == CheckStatus.Failed }
            val reason = if (offline) {
                "No internet connection. Connect to the internet and try again."
            } else {
                "Cloud streaming is not available in this build yet. A Nebula backend, a gaming host and the Moonlight client are still needed."
            }
            local.update { it.copy(phase = StreamingState.Unavailable(checks, reason)) }
        }
    }

    fun reset() {
        local.update { it.copy(phase = StreamingState.Idle) }
    }

    fun exitSession() {
        streaming.client.stop()
        reset()
    }
}
