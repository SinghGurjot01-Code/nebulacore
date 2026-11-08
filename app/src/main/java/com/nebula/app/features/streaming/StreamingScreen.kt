package com.nebula.app.features.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.GameImage
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.LoadingState
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaCard
import com.nebula.app.design.components.NebulaTopBar
import com.nebula.app.design.components.RowDivider
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.models.CheckStatus
import com.nebula.app.domain.models.PreflightCheck
import com.nebula.app.domain.models.StreamingState

@Composable
fun StreamingScreen(gameId: String, onExit: () -> Unit, onBook: () -> Unit) {
    val viewModel = nebulaViewModel(key = "stream-$gameId") {
        StreamingViewModel(it.gameRepository, it.streamingRepository, gameId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val phase = state.phase

    SessionDisplayMode(enabled = phase is StreamingState.Streaming)

    Column(modifier = Modifier.fillMaxSize()) {
        if (phase !is StreamingState.Streaming) {
            NebulaTopBar("Launch game", onBack = onExit)
        }
        val game = state.game
        when {
            state.loading -> LoadingState()
            game == null -> ErrorState("This game could not be found.", onRetry = null)
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (phase !is StreamingState.Streaming) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    ) {
                        GameImage(game.heroUrl, game.title, Modifier.fillMaxSize())
                    }
                    Text(game.title, style = MaterialTheme.typography.headlineSmall)
                }
                when (phase) {
                    is StreamingState.Idle -> {
                        InfoBanner(
                            text = "Nebula will check your connection and reserve a gaming host before streaming starts. " +
                                "Hosts are not connected in this build, so the checks will end with an explanation.",
                            icon = Icons.Filled.CloudOff
                        )
                        NebulaButton(
                            text = "Prepare session",
                            onClick = viewModel::prepare,
                            modifier = Modifier.fillMaxWidth(),
                            kind = ButtonKind.Accent,
                            icon = Icons.Filled.PlayArrow
                        )
                        NebulaButton("Book a session instead", onBook, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                    }
                    is StreamingState.Preparing -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = NebulaColors.Accent, strokeWidth = 2.dp)
                            Text("Running pre-session checks", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    is StreamingState.Unavailable -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(NebulaColors.Subtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = NebulaColors.Secondary)
                            }
                            Text("Streaming is not available yet", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(phase.reason, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
                        ChecksCard(phase.checks)
                        NebulaButton("Return to Nebula", onExit, Modifier.fillMaxWidth(), kind = ButtonKind.Primary)
                        NebulaButton("Book a session", onBook, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                        NebulaButton(
                            "Run checks again",
                            viewModel::prepare,
                            Modifier.fillMaxWidth(),
                            kind = ButtonKind.Secondary,
                            icon = Icons.Filled.Refresh
                        )
                    }
                    is StreamingState.Disconnected -> {
                        Text("Session ended", style = MaterialTheme.typography.titleLarge)
                        Text(phase.reason, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
                        NebulaButton("Return to Nebula", onExit, Modifier.fillMaxWidth(), kind = ButtonKind.Primary)
                    }
                    is StreamingState.Streaming -> {
                        // Reached only when a real streaming client is integrated.
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Streaming ${game.title}", style = MaterialTheme.typography.titleLarge)
                                NebulaButton("Exit session", {
                                    viewModel.exitSession()
                                    onExit()
                                }, kind = ButtonKind.Danger)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChecksCard(checks: List<PreflightCheck>) {
    NebulaCard(modifier = Modifier.fillMaxWidth()) {
        checks.forEachIndexed { index, check ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                when (check.status) {
                    CheckStatus.Passed -> Icon(Icons.Filled.CheckCircle, null, tint = NebulaColors.Success)
                    CheckStatus.Failed -> Icon(Icons.Filled.Error, null, tint = NebulaColors.Error)
                    CheckStatus.Pending -> Icon(Icons.Filled.Schedule, null, tint = NebulaColors.Secondary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(check.title, style = MaterialTheme.typography.titleSmall)
                    Text(check.detail, style = MaterialTheme.typography.bodySmall, color = NebulaColors.Secondary)
                }
                StatusChip(
                    text = when (check.status) {
                        CheckStatus.Passed -> "Passed"
                        CheckStatus.Failed -> "Failed"
                        CheckStatus.Pending -> "Not available"
                    },
                    tone = when (check.status) {
                        CheckStatus.Passed -> ChipTone.Success
                        CheckStatus.Failed -> ChipTone.Danger
                        CheckStatus.Pending -> ChipTone.Neutral
                    }
                )
            }
            if (index < checks.lastIndex) RowDivider()
        }
    }
}
