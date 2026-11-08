package com.nebula.app.features.support

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.LocalSnackbarHost
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.formatTimestamp
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.DemoBadge
import com.nebula.app.design.components.EmptyState
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.LoadingState
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaCard
import com.nebula.app.design.components.NebulaTextField
import com.nebula.app.design.components.NebulaTopBar
import com.nebula.app.design.components.ShimmerBox
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.models.MessageAuthor
import com.nebula.app.domain.models.SupportTicket
import com.nebula.app.domain.models.TicketStatus
import kotlinx.coroutines.launch

private fun ticketTone(status: TicketStatus): ChipTone = when (status) {
    TicketStatus.Open -> ChipTone.Accent
    TicketStatus.InProgress -> ChipTone.Warning
    TicketStatus.Resolved -> ChipTone.Success
}

private const val DEMO_TICKET_NOTE =
    "Demo tickets are stored only on this device. No real support team receives them."

private const val REAL_TICKET_NOTE =
    "Tickets are saved to your Nebula account. The support team is not live yet, so replies may take a while."

@Composable
fun SupportScreen(
    innerPadding: PaddingValues,
    onNewTicket: () -> Unit,
    onOpenTicket: (String) -> Unit
) {
    val viewModel = nebulaViewModel { SupportViewModel(it.supportRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val demo = LocalDemoMode.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Support", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (demo) DemoBadge()
            }
            InfoBanner(if (demo) DEMO_TICKET_NOTE else REAL_TICKET_NOTE, Icons.Filled.Info)
            NebulaButton(
                text = "Create a ticket",
                onClick = onNewTicket,
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Accent,
                icon = Icons.Filled.Add
            )
        }

        when (val current = state) {
            is Loadable.Loading -> Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(Modifier.fillMaxWidth().height(84.dp))
                ShimmerBox(Modifier.fillMaxWidth().height(84.dp))
            }
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                if (current.value.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.SupportAgent,
                        title = "No tickets yet",
                        message = if (demo) "Create a demo ticket to see how support will work." else "Create a ticket if you need help.",
                        actionLabel = "Create a ticket",
                        onAction = onNewTicket
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(current.value, key = { it.id }) { ticket -> TicketCard(ticket) { onOpenTicket(ticket.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: SupportTicket, onClick: () -> Unit) {
    NebulaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticket.displayId + " - " + ticket.category, style = MaterialTheme.typography.labelMedium, color = NebulaColors.Secondary, modifier = Modifier.weight(1f))
                StatusChip(ticket.status.label, ticketTone(ticket.status))
            }
            Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
            Text(
                formatTimestamp(ticket.createdAt) + " - " + ticket.messages.size + " messages",
                style = MaterialTheme.typography.bodySmall,
                color = NebulaColors.Secondary
            )
        }
    }
}

@Composable
fun CreateTicketScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val viewModel = nebulaViewModel { CreateTicketViewModel(it.supportRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val demo = LocalDemoMode.current

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        NebulaTopBar("New ticket", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoBanner(if (demo) DEMO_TICKET_NOTE else REAL_TICKET_NOTE, Icons.Filled.Info)
            Text("Category", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.categories) { category ->
                    val selected = category == state.category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) NebulaColors.Ink else NebulaColors.Surface)
                            .clickable { viewModel.onCategory(category) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            category,
                            color = if (selected) Color.White else NebulaColors.Ink,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            NebulaTextField(
                value = state.subject,
                onValueChange = viewModel::onSubject,
                label = "Subject",
                error = state.subjectError,
                enabled = !state.loading
            )
            NebulaTextField(
                value = state.description,
                onValueChange = viewModel::onDescription,
                label = "Describe the issue",
                error = state.descriptionError,
                singleLine = false,
                minLines = 5,
                imeAction = ImeAction.Default,
                enabled = !state.loading
            )
            state.formError?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
            NebulaButton(
                text = if (demo) "Submit demo ticket" else "Submit ticket",
                onClick = { viewModel.submit(onCreated) },
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Accent,
                loading = state.loading
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun TicketDetailScreen(ticketId: String, onBack: () -> Unit) {
    val viewModel = nebulaViewModel(key = "ticket-$ticketId") { TicketDetailViewModel(it.supportRepository, ticketId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val demo = LocalDemoMode.current

    val messageCount = (state as? Loadable.Ready)?.value?.messages?.size ?: 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) listState.animateScrollToItem(messageCount + 1)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        NebulaTopBar("Ticket", onBack = onBack)
        when (val current = state) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                val ticket = current.value
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(ticket.status.label, ticketTone(ticket.status))
                                StatusChip(ticket.category, ChipTone.Neutral)
                                if (demo) DemoBadge(text = "Local demo")
                            }
                            Text(ticket.subject, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                ticket.displayId + " - opened " + formatTimestamp(ticket.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = NebulaColors.Secondary
                            )
                        }
                    }
                    items(ticket.messages, key = { it.id }) { message ->
                        val mine = message.author == MessageAuthor.User
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 300.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (mine) NebulaColors.Ink else NebulaColors.Surface)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    message.text,
                                    color = if (mine) Color.White else NebulaColors.Ink,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                (if (mine) "You" else if (demo) "Support (sample)" else "Support") + " - " + formatTimestamp(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = NebulaColors.Secondary,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NebulaColors.Surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NebulaTextField(
                        value = draft,
                        onValueChange = viewModel::onDraftChange,
                        label = "Write a reply",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Send,
                        enabled = !sending,
                        onImeAction = { viewModel.send { msg -> scope.launch { snackbar.showSnackbar(msg) } } }
                    )
                    IconButton(
                        onClick = { viewModel.send { msg -> scope.launch { snackbar.showSnackbar(msg) } } },
                        enabled = draft.isNotBlank() && !sending
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send reply",
                            tint = if (draft.isNotBlank()) NebulaColors.Accent else NebulaColors.Secondary
                        )
                    }
                }
            }
        }
    }
}
