package com.nebula.app.data.repositories

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nebula.app.domain.models.CheckStatus
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.PreflightCheck
import com.nebula.app.domain.models.StreamSession
import com.nebula.app.domain.models.StreamingState
import com.nebula.app.domain.repositories.StreamingClient
import com.nebula.app.domain.repositories.StreamingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun interface ConnectivityChecker {
    fun isOnline(): Boolean
}

class AndroidConnectivityChecker(context: Context) : ConnectivityChecker {
    private val manager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

/**
 * Placeholder for the future Moonlight-based client. It never streams anything:
 * [start] reports that no streaming host is connected.
 */
class UnavailableStreamingClient : StreamingClient {
    private val mutableState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    override val state: StateFlow<StreamingState> = mutableState.asStateFlow()

    override suspend fun start(session: StreamSession) {
        mutableState.value = StreamingState.Disconnected("No streaming client is integrated in this build.")
    }

    override fun stop() {
        mutableState.value = StreamingState.Idle
    }
}

class LocalStreamingRepository(
    private val connectivity: ConnectivityChecker,
    override val client: StreamingClient = UnavailableStreamingClient()
) : StreamingRepository {

    override suspend fun runPreflight(game: Game): List<PreflightCheck> {
        val online = connectivity.isOnline()
        return listOf(
            PreflightCheck(
                id = "network",
                title = "Network connection",
                detail = if (online) "Your device is online." else "No internet connection detected.",
                status = if (online) CheckStatus.Passed else CheckStatus.Failed
            ),
            PreflightCheck(
                id = "session",
                title = "Session validation",
                detail = "Needs the Nebula backend, which is not connected yet.",
                status = CheckStatus.Pending
            ),
            PreflightCheck(
                id = "host",
                title = "Gaming host assignment",
                detail = "Needs a Sunshine-compatible host and the Nebula backend.",
                status = CheckStatus.Pending
            ),
            PreflightCheck(
                id = "client",
                title = "Streaming client",
                detail = "Moonlight is not integrated in this build.",
                status = CheckStatus.Pending
            )
        )
    }
}
