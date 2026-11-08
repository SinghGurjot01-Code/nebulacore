package com.nebula.app.data

import com.nebula.app.data.repositories.LocalSupportRepository
import com.nebula.app.domain.models.MessageAuthor
import com.nebula.app.domain.models.TicketStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSupportRepositoryTest {

    private fun repository() = LocalSupportRepository(simulatedLatencyMs = 0)

    @Test
    fun createdTicketIsOpenAndListedFirst() = runTest {
        val repo = repository()
        val ticket = repo.createTicket("Account", "Cannot log in", "I forgot which email I used.").getOrThrow()
        assertEquals(TicketStatus.Open, ticket.status)
        assertEquals(ticket.id, repo.tickets.first().first().id)
    }

    @Test
    fun newTicketCarriesTheDemoNotice() = runTest {
        val repo = repository()
        val ticket = repo.createTicket("Bookings", "Slot missing", "The 18:00 slot is gone.").getOrThrow()
        val support = ticket.messages.first { it.author == MessageAuthor.Support }
        assertTrue(support.text.contains("No support team"))
    }

    @Test
    fun replyIsAppendedAndReopensResolvedTickets() = runTest {
        val repo = repository()
        val resolved = repo.tickets.first().first { it.status == TicketStatus.Resolved }
        assertTrue(repo.addReply(resolved.id, "Thanks, one more question.").isSuccess)
        val updated = repo.ticket(resolved.id).first()
        assertEquals(TicketStatus.Open, updated?.status)
        assertEquals(resolved.messages.size + 1, updated?.messages?.size)
    }

    @Test
    fun blankReplyAndUnknownTicketFail() = runTest {
        val repo = repository()
        val id = repo.tickets.first().first().id
        assertTrue(repo.addReply(id, "   ").isFailure)
        assertTrue(repo.addReply("missing", "Hello").isFailure)
    }

    @Test
    fun hasCategoriesAndSampleTickets() = runTest {
        val repo = repository()
        assertTrue(repo.categories.isNotEmpty())
        assertTrue(repo.tickets.first().size >= 2)
    }
}
