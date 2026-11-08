package com.nebula.app.data.repositories

import com.nebula.app.data.local.DemoContent
import com.nebula.app.domain.models.MessageAuthor
import com.nebula.app.domain.models.SupportTicket
import com.nebula.app.domain.models.TicketMessage
import com.nebula.app.domain.models.TicketStatus
import com.nebula.app.domain.repositories.NebulaException
import com.nebula.app.domain.repositories.SupportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** Local demo tickets. Nothing is sent to a support team in this phase. */
class LocalSupportRepository(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val simulatedLatencyMs: Long = 500L
) : SupportRepository {

    override val categories: List<String> = DemoContent.supportCategories

    private var nextTicket = 1003
    private var nextMessage = 1
    private val store = MutableStateFlow(seed())

    override val tickets: Flow<List<SupportTicket>> =
        store.map { list -> list.sortedByDescending { it.createdAt } }

    override fun ticket(id: String): Flow<SupportTicket?> =
        store.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createTicket(
        category: String,
        subject: String,
        description: String
    ): Result<SupportTicket> {
        delay(simulatedLatencyMs)
        val now = clock.millis()
        val ticket = SupportTicket(
            id = "T-${nextTicket++}",
            category = category,
            subject = subject.trim(),
            description = description.trim(),
            status = TicketStatus.Open,
            createdAt = now,
            messages = listOf(
                TicketMessage(newMessageId(), MessageAuthor.User, description.trim(), now),
                TicketMessage(
                    newMessageId(), MessageAuthor.Support,
                    "Demo ticket: this ticket is stored only on your device. No support team has received it.",
                    now + 1
                )
            )
        )
        store.value = store.value + ticket
        return Result.success(ticket)
    }

    override suspend fun addReply(ticketId: String, text: String): Result<Unit> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(NebulaException("Write a message first."))
        val ticket = store.value.firstOrNull { it.id == ticketId }
            ?: return Result.failure(NebulaException("Ticket not found."))
        val message = TicketMessage(newMessageId(), MessageAuthor.User, trimmed, clock.millis())
        store.value = store.value.map {
            if (it.id != ticketId) it else it.copy(
                messages = it.messages + message,
                status = if (ticket.status == TicketStatus.Resolved) TicketStatus.Open else ticket.status
            )
        }
        return Result.success(Unit)
    }

    private fun newMessageId() = "m${nextMessage++}"

    private fun seed(): List<SupportTicket> {
        val now = clock.millis()
        val day = 24L * 60 * 60 * 1000
        return listOf(
            SupportTicket(
                id = "T-1002", category = "Technical issue",
                subject = "Artwork is slow to load",
                description = "Game covers take a long time to appear on mobile data.",
                status = TicketStatus.InProgress, createdAt = now - 2 * day,
                messages = listOf(
                    TicketMessage("s1", MessageAuthor.User, "Game covers take a long time to appear on mobile data.", now - 2 * day),
                    TicketMessage("s2", MessageAuthor.Support, "Sample reply: thanks, we are looking into image caching.", now - 2 * day + 3_600_000)
                )
            ),
            SupportTicket(
                id = "T-1001", category = "Account",
                subject = "How do I change my display name?",
                description = "I would like to update the name shown on my profile.",
                status = TicketStatus.Resolved, createdAt = now - 6 * day,
                messages = listOf(
                    TicketMessage("s3", MessageAuthor.User, "I would like to update the name shown on my profile.", now - 6 * day),
                    TicketMessage("s4", MessageAuthor.Support, "Sample reply: open Profile, then Edit profile.", now - 6 * day + 1_800_000)
                )
            )
        )
    }
}
