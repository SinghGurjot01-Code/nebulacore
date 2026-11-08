package com.nebula.app.features.bookings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.LocalSnackbarHost
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.formatDuration
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.core.common.pretty
import com.nebula.app.core.common.short
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.ConfirmDialog
import com.nebula.app.design.components.DemoBadge
import com.nebula.app.design.components.EmptyState
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.LoadingState
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaCard
import com.nebula.app.design.components.NebulaTopBar
import com.nebula.app.design.components.ShimmerBox
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingStatus
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun statusTone(status: BookingStatus): ChipTone = when (status) {
    BookingStatus.Upcoming -> ChipTone.Accent
    BookingStatus.Completed -> ChipTone.Success
    BookingStatus.Cancelled -> ChipTone.Neutral
}

private const val SIMULATED_NOTE =
    "This is a simulated booking stored on your device. No server reservation exists, and nothing is charged."

private const val REAL_NOTE =
    "Your booking is saved to your Nebula account. Cloud streaming is not available yet, so no game session starts."

// ------------------------------------------------------------------ Overview

@Composable
fun BookingsScreen(
    innerPadding: PaddingValues,
    onNewBooking: () -> Unit,
    onOpenBooking: (String) -> Unit
) {
    val viewModel = nebulaViewModel { BookingsViewModel(it.bookingRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    val demo = LocalDemoMode.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bookings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (demo) DemoBadge()
            }
            NebulaButton(
                text = "Book a session",
                onClick = onNewBooking,
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Accent,
                icon = Icons.Filled.Add
            )
            val upcomingCount = (state as? Loadable.Ready)?.value?.upcoming?.size ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NebulaColors.Subtle)
                    .padding(4.dp)
            ) {
                listOf("Upcoming ($upcomingCount)", "History").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (tab == index) NebulaColors.Surface else Color.Transparent)
                            .clickable { tab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tab == index) NebulaColors.Ink else NebulaColors.Secondary
                        )
                    }
                }
            }
        }

        when (val current = state) {
            is Loadable.Loading -> Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(Modifier.fillMaxWidth().height(96.dp))
                ShimmerBox(Modifier.fillMaxWidth().height(96.dp))
            }
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                val list = if (tab == 0) current.value.upcoming else current.value.history
                if (list.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.CalendarMonth,
                        title = if (tab == 0) "No upcoming bookings" else "No booking history yet",
                        message = if (tab == 0) (if (demo) "Book a simulated session to see it here." else "Book a session to see it here.")
                        else "Completed and cancelled bookings will appear here.",
                        actionLabel = if (tab == 0) "Book a session" else null,
                        onAction = if (tab == 0) onNewBooking else null
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { booking ->
                            BookingCard(booking) { onOpenBooking(booking.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking, onClick: () -> Unit) {
    NebulaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    booking.gameTitle ?: booking.optionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(booking.status.label, statusTone(booking.status))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp), tint = NebulaColors.Secondary)
                Text(booking.date.pretty(), style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AccessTime, null, Modifier.size(16.dp), tint = NebulaColors.Secondary)
                Text(
                    booking.timeLabel + " - " + formatDuration(booking.durationMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NebulaColors.Secondary
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Plans

@Composable
fun BookingPlansScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    val viewModel = nebulaViewModel { BookingPlansViewModel(it.bookingRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val demo = LocalDemoMode.current
    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Choose a session", onBack = onBack)
        when (val current = state) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Failed -> ErrorState(current.message, onRetry = viewModel::load)
            is Loadable.Ready -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoBanner(if (demo) SIMULATED_NOTE else REAL_NOTE, Icons.Filled.Info)
                current.value.forEach { option ->
                    NebulaCard(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(option.id) }) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(option.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusChip(formatDuration(option.durationMinutes), ChipTone.Accent)
                            }
                            Text(option.description, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ Date, time and game

@Composable
fun BookingSessionScreen(
    optionId: String,
    initialGameId: String?,
    onBack: () -> Unit,
    onReview: (LocalDate, Int, String?) -> Unit
) {
    val viewModel = nebulaViewModel(key = "session-$optionId-$initialGameId") {
        BookingSessionViewModel(it.bookingRepository, it.gameRepository, optionId, initialGameId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingGame by remember { mutableStateOf(false) }
    val option = state.option

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar(option?.title ?: "Session", onBack = onBack)
        if (state.loading) {
            LoadingState()
            return@Column
        }
        val loadError = state.error
        if (loadError != null) {
            ErrorState(loadError, onRetry = viewModel::load)
            return@Column
        }
        if (option == null) {
            ErrorState("This session option no longer exists.", onRetry = null)
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pick a date", style = MaterialTheme.typography.titleMedium)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.dates, key = { it.toString() }) { date ->
                    val selected = date == state.selectedDate
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) NebulaColors.Ink else NebulaColors.Surface)
                            .clickable { viewModel.selectDate(date) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val parts = date.short().split(" ")
                        Text(
                            parts.firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White.copy(alpha = 0.8f) else NebulaColors.Secondary
                        )
                        Text(
                            parts.getOrNull(1).orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) Color.White else NebulaColors.Ink
                        )
                        Text(
                            parts.getOrNull(2).orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White.copy(alpha = 0.8f) else NebulaColors.Secondary
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pick a time", style = MaterialTheme.typography.titleMedium)
                if (state.slotsLoading) {
                    Text("Loading times...", style = MaterialTheme.typography.bodySmall, color = NebulaColors.Secondary)
                }
                state.slots.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { slot ->
                            val selected = state.selectedStart == slot.startMinutes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            selected -> NebulaColors.Accent
                                            slot.available -> NebulaColors.Surface
                                            else -> NebulaColors.Subtle
                                        }
                                    )
                                    .clickable(enabled = slot.available) { viewModel.selectSlot(slot.startMinutes) }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (slot.available) slot.label else slot.label + " (full)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = when {
                                        selected -> Color.White
                                        slot.available -> NebulaColors.Ink
                                        else -> NebulaColors.Secondary
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (!state.slotsLoading && state.slots.none { it.available }) {
                    Text(
                        "No slots are free on this date. Try another day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NebulaColors.Secondary
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Game (optional)", style = MaterialTheme.typography.titleMedium)
                NebulaCard(modifier = Modifier.fillMaxWidth(), onClick = { pickingGame = true }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = NebulaColors.Accent)
                        Text(
                            state.selectedGame?.title ?: "No specific game",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Change", color = NebulaColors.Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            val start = state.selectedStart
            NebulaButton(
                text = if (start == null) "Select a time to continue" else "Review booking",
                onClick = { if (start != null) onReview(state.selectedDate, start, state.selectedGame?.id) },
                modifier = Modifier.fillMaxWidth(),
                enabled = start != null,
                kind = ButtonKind.Accent
            )
        }
    }

    if (pickingGame) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingGame = false },
            containerColor = NebulaColors.Surface,
            title = { Text("Choose a game") },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    item {
                        Column {
                            TextButton(onClick = { viewModel.selectGame(null); pickingGame = false }) {
                                Text("No specific game", color = NebulaColors.Ink)
                            }
                            HorizontalDivider(color = NebulaColors.Border)
                        }
                    }
                    items(state.games, key = { it.id }) { game ->
                        TextButton(onClick = { viewModel.selectGame(game); pickingGame = false }) {
                            Text(game.title, color = NebulaColors.Ink, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingGame = false }) { Text("Close", color = NebulaColors.Secondary) }
            }
        )
    }
}

// ------------------------------------------------------------------ Review

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = NebulaColors.Secondary, modifier = Modifier.width(110.dp))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
fun BookingReviewScreen(
    optionId: String,
    date: LocalDate,
    startMinutes: Int,
    gameId: String?,
    onBack: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val viewModel = nebulaViewModel(key = "review-$optionId-$date-$startMinutes-$gameId") {
        BookingReviewViewModel(it.bookingRepository, it.gameRepository, optionId, date, startMinutes, gameId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val option = state.option
    val demo = LocalDemoMode.current

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Review booking", onBack = onBack)
        if (state.loading) {
            LoadingState()
            return@Column
        }
        if (option == null) {
            ErrorState(state.error ?: "This session option no longer exists.", onRetry = null)
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NebulaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SummaryRow("Session", option.title)
                    HorizontalDivider(color = NebulaColors.Border)
                    SummaryRow("Game", state.game?.title ?: "No specific game")
                    HorizontalDivider(color = NebulaColors.Border)
                    SummaryRow("Date", date.pretty())
                    HorizontalDivider(color = NebulaColors.Border)
                    SummaryRow(
                        "Time",
                        com.nebula.app.domain.models.formatMinutes(startMinutes) + " - " +
                            com.nebula.app.domain.models.formatMinutes(startMinutes + option.durationMinutes)
                    )
                    HorizontalDivider(color = NebulaColors.Border)
                    SummaryRow("Length", formatDuration(option.durationMinutes))
                }
            }
            InfoBanner(if (demo) SIMULATED_NOTE else REAL_NOTE, Icons.Filled.Info)
            state.error?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            NebulaButton(
                text = if (demo) "Confirm demo booking" else "Confirm booking",
                onClick = { viewModel.confirm(onConfirmed) },
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Accent,
                loading = state.submitting
            )
        }
    }
}

// ------------------------------------------------------------------ Confirmation

@Composable
fun BookingConfirmedScreen(bookingId: String, onViewBooking: () -> Unit, onDone: () -> Unit) {
    val viewModel = nebulaViewModel(key = "done-$bookingId") { BookingConfirmedViewModel(it.bookingRepository, bookingId) }
    val state by viewModel.booking.collectAsStateWithLifecycle()
    val demo = LocalDemoMode.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NebulaColors.SuccessSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = NebulaColors.Success, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(if (demo) "Demo booking confirmed" else "Booking confirmed", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        (state as? Loadable.Ready)?.value?.let { booking ->
            Text(
                (booking.gameTitle ?: booking.optionTitle) + "\n" + booking.date.pretty() + ", " + booking.timeLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = NebulaColors.Secondary,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))
        InfoBanner(if (demo) SIMULATED_NOTE else REAL_NOTE, Icons.Filled.Info)
        Spacer(Modifier.height(24.dp))
        NebulaButton("View booking", onViewBooking, Modifier.fillMaxWidth(), kind = ButtonKind.Primary)
        Spacer(Modifier.height(10.dp))
        NebulaButton("Back to bookings", onDone, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
    }
}

// ------------------------------------------------------------------ Details

@Composable
fun BookingDetailScreen(bookingId: String, onBack: () -> Unit, onOpenGame: (String) -> Unit) {
    val viewModel = nebulaViewModel(key = "booking-$bookingId") { BookingDetailViewModel(it.bookingRepository, bookingId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cancelling by viewModel.cancelling.collectAsStateWithLifecycle()
    var confirmCancel by remember { mutableStateOf(false) }
    val demo = LocalDemoMode.current
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Booking details", onBack = onBack)
        when (val current = state) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                val booking = current.value.booking
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(booking.status.label, statusTone(booking.status))
                        if (demo) DemoBadge()
                    }
                    Text(booking.gameTitle ?: booking.optionTitle, style = MaterialTheme.typography.headlineSmall)
                    NebulaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SummaryRow("Booking", booking.id)
                            HorizontalDivider(color = NebulaColors.Border)
                            SummaryRow("Session", booking.optionTitle)
                            HorizontalDivider(color = NebulaColors.Border)
                            SummaryRow("Date", booking.date.pretty())
                            HorizontalDivider(color = NebulaColors.Border)
                            SummaryRow("Time", booking.timeLabel)
                            HorizontalDivider(color = NebulaColors.Border)
                            SummaryRow("Length", formatDuration(booking.durationMinutes))
                        }
                    }
                    InfoBanner(if (demo) SIMULATED_NOTE else REAL_NOTE, Icons.Filled.Info)
                    booking.gameId?.let { id ->
                        NebulaButton(
                            "View game",
                            { onOpenGame(id) },
                            Modifier.fillMaxWidth(),
                            kind = ButtonKind.Secondary,
                            icon = Icons.Filled.SportsEsports
                        )
                    }
                    if (booking.status == BookingStatus.Upcoming) {
                        NebulaButton(
                            text = "Cancel booking",
                            onClick = { confirmCancel = true },
                            modifier = Modifier.fillMaxWidth(),
                            kind = ButtonKind.Danger,
                            enabled = current.value.cancellable,
                            loading = cancelling
                        )
                        if (!current.value.cancellable) {
                            Text(
                                "Bookings can only be cancelled up to ${Booking.MIN_CANCEL_NOTICE_MINUTES} minutes before they start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NebulaColors.Secondary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            title = "Cancel this booking?",
            message = if (demo) "The simulated booking will be marked as cancelled. This cannot be undone." else "Your booking will be cancelled. This cannot be undone.",
            confirmText = "Cancel booking",
            dismissText = "Keep booking",
            destructive = true,
            onConfirm = {
                confirmCancel = false
                viewModel.cancel { result ->
                    scope.launch {
                        snackbar.showSnackbar(
                            result.fold(
                                onSuccess = { "Booking cancelled." },
                                onFailure = { it.message ?: "Could not cancel the booking." }
                            ),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onDismiss = { confirmCancel = false }
        )
    }
}
