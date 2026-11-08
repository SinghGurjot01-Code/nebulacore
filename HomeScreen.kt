package com.nebula.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.formatDuration
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.core.common.pretty
import com.nebula.app.core.common.relativeTo
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.DemoBadge
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.GameHeroCard
import com.nebula.app.design.components.GameTile
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaCard
import com.nebula.app.design.components.NebulaWordmark
import com.nebula.app.design.components.SectionHeader
import com.nebula.app.design.components.ShimmerBox
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.models.StreamingState
import java.time.LocalDate

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onOpenGame: (String) -> Unit,
    onBrowseGames: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onBookSession: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val viewModel = nebulaViewModel {
        HomeViewModel(
            it.authRepository, it.gameRepository, it.bookingRepository,
            it.profileRepository, it.streamingRepository
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    when (val current = state) {
        is Loadable.Loading -> HomeLoading(innerPadding)
        is Loadable.Failed -> Box(Modifier.fillMaxSize().padding(innerPadding)) {
            ErrorState(current.message, onRetry = viewModel::retry)
        }
        is Loadable.Ready -> HomeContentView(
            content = current.value,
            sessionState = sessionState,
            innerPadding = innerPadding,
            onToggleFavourite = viewModel::toggleFavourite,
            onOpenGame = onOpenGame,
            onBrowseGames = onBrowseGames,
            onOpenBooking = onOpenBooking,
            onBookSession = onBookSession,
            onOpenNotifications = onOpenNotifications,
            onOpenProfile = onOpenProfile
        )
    }
}

@Composable
private fun HomeLoading(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(40.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(110.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(90.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(170.dp))
    }
}

@Composable
private fun HomeContentView(
    content: HomeContent,
    sessionState: StreamingState,
    innerPadding: PaddingValues,
    onToggleFavourite: (String) -> Unit,
    onOpenGame: (String) -> Unit,
    onBrowseGames: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onBookSession: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val today = LocalDate.now()
    val demo = LocalDemoMode.current
    val firstName = content.user?.fullName?.trim()?.substringBefore(" ").orEmpty().ifEmpty { "there" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NebulaWordmark(modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    if (content.unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 10.dp, end = 10.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NebulaColors.Accent)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NebulaColors.Ink)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        content.user?.initials ?: "N",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome back, $firstName", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Pick a game, plan a session and get ready to play.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NebulaColors.Secondary
                )
            }
        }

        item { SessionStatusCard(sessionState, onBrowseGames, Modifier.padding(horizontal = 20.dp)) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Upcoming booking")
                val booking = content.upcoming
                if (booking != null) {
                    NebulaCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenBooking(booking.id) }) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NebulaColors.AccentSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = NebulaColors.Accent)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(booking.gameTitle ?: booking.optionTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    booking.date.relativeTo(today).let { if (booking.date.isAfter(today)) booking.date.pretty() else it } +
                                        " - " + booking.timeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NebulaColors.Secondary
                                )
                                Text(
                                    formatDuration(booking.durationMinutes) + " - " + booking.optionTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NebulaColors.Secondary
                                )
                            }
                            if (demo) DemoBadge()
                        }
                    }
                } else {
                    NebulaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("No upcoming bookings", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (demo) "Reserve a simulated session to see how booking will work." else "Reserve a session for a game you want to play.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NebulaColors.Secondary
                            )
                            NebulaButton("Book a session", onBookSession, kind = ButtonKind.Primary)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    "Featured games",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    actionLabel = "See all",
                    onAction = onBrowseGames
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(content.featured, key = { it.id }) { game ->
                        GameHeroCard(
                            game = game,
                            onClick = { onOpenGame(game.id) },
                            modifier = Modifier.width(280.dp).height(158.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Recently played", modifier = Modifier.padding(horizontal = 20.dp))
                val recent = content.recent.filter { it.game != null }
                if (recent.isEmpty()) {
                    Text(
                        "Games you play will appear here.",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = NebulaColors.Secondary
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recent, key = { it.session.id }) { item ->
                            val game = item.game ?: return@items
                            Column(modifier = Modifier.width(132.dp)) {
                                GameTile(
                                    game = game,
                                    isFavourite = game.id in content.favouriteIds,
                                    onClick = { onOpenGame(game.id) },
                                    onToggleFavourite = { onToggleFavourite(game.id) }
                                )
                                Text(
                                    item.session.playedOn.relativeTo(today) + (if (demo) " (demo)" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NebulaColors.Secondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Announcements")
                content.announcements.forEach { announcement ->
                    NebulaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = NebulaColors.Accent)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(announcement.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    announcement.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NebulaColors.Secondary
                                )
                            }
                        }
                    }
                }
                NebulaButton(
                    text = "Browse all games",
                    onClick = onBrowseGames,
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Primary
                )
            }
        }
    }
}

@Composable
private fun SessionStatusCard(state: StreamingState, onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    val (label, tone, detail) = when (state) {
        is StreamingState.Streaming -> Triple("Streaming", ChipTone.Success, "A cloud session is active.")
        is StreamingState.Preparing -> Triple("Preparing", ChipTone.Warning, "Checking your connection.")
        is StreamingState.Unavailable -> Triple("Unavailable", ChipTone.Neutral, state.reason)
        is StreamingState.Disconnected -> Triple("Disconnected", ChipTone.Neutral, state.reason)
        is StreamingState.Idle -> Triple(
            "No active session", ChipTone.Neutral,
            "Cloud streaming is not connected in this build. You can still browse games and book a simulated session."
        )
    }
    NebulaCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Cloud, contentDescription = null, tint = NebulaColors.Accent)
                Text("Gaming session", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(label, tone)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
            NebulaButton("Browse games", onBrowse, kind = ButtonKind.Secondary)
        }
    }
}
