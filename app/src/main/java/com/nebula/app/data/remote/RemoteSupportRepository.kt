package com.nebula.app.data.remote

import com.nebula.app.data.local.DemoContent
import com.nebula.app.domain.models.MessageAuthor
import com.nebula.app.domain.models.SupportTicket
import com.nebula.app.domain.models.TicketMessage
import com.nebula.app.domain.models.TicketStatus
import com.nebula.app.domain.repositories.NebulaException
import com.nebula.app.domain.repositories.SupportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/** Support tickets stored in Supabase. Replies from the Nebula team are written by staff in the dashboard. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSupportRepository(private val client: SupabaseClient) : SupportRepository {

    override val isSimulated: Boolean = false
    override val categories: List<String> = DemoContent.supportCategories

    private val reload = MutableStateFlow(0)

    override val tickets: Flow<List<SupportTicket>> = reload.flatMapLatest { flow { emit(fetchTickets()) } }

    override fun ticket(id: String): Flow<SupportTicket?> =
        reload.flatMapLatest { flow { emit(fetchTickets().firstOrNull { it.id == id }) } }

    override suspend fun createTicket(
        category: String,
        subject: String,
        description: String
    ): Result<SupportTicket> = safeResult {
        val body = JSONObject()
            .put("p_category", category)
            .put("p_subject", subject)
            .put("p_description", description)
            .toString()
        val response = client.request("POST", "/rest/v1/rpc/create_ticket", body)
        if (!response.ok) throw NebulaException(response.errorMessage("Could not create the ticket."))
        reload.value = reload.value + 1
        toTicket(response.json(), emptyList())
    }

    override suspend fun addReply(ticketId: String, text: String): Result<Unit> = safeResult {
        if (text.isBlank()) throw NebulaException("Write a message first.")
        val body = JSONObject()
            .put("ticket_id", ticketId)
            .put("author", "user")
            .put("body", text.trim())
            .toString()
        val response = client.request(
            "POST", "/rest/v1/ticket_messages", body, mapOf("Prefer" to "return=minimal")
        )
        if (!response.ok) throw NebulaException(response.errorMessage("Could not send your reply."))
        reload.value = reload.value + 1
    }

    private suspend fun fetchTickets(): List<SupportTicket> {
        val response = client.request(
            "GET",
            "/rest/v1/support_tickets?select=*,ticket_messages(id,author,body,created_at)&order=created_at.desc"
        )
        if (response.code == 401) throw NebulaException("Please log in to see your tickets.")
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load your tickets."))
        val array = JSONArray(response.body)
        return (0 until array.length()).map { index ->
            val o = array.getJSONObject(index)
            val rawMessages = o.optJSONArray("ticket_messages") ?: JSONArray()
            val messages = (0 until rawMessages.length()).map { m ->
                val message = rawMessages.getJSONObject(m)
                TicketMessage(
                    id = message.getString("id"),
                    author = if (message.getString("author") == "support") MessageAuthor.Support else MessageAuthor.User,
                    text = message.getString("body"),
                    timestamp = OffsetDateTime.parse(message.getString("created_at")).toInstant().toEpochMilli()
                )
            }.sortedBy { it.timestamp }
            toTicket(o, messages)
        }
    }

    private fun toTicket(o: JSONObject, messages: List<TicketMessage>): SupportTicket = SupportTicket(
        id = o.getString("id"),
        category = o.getString("category"),
        subject = o.getString("subject"),
        description = o.getString("description"),
        status = when (o.getString("status")) {
            "in_progress" -> TicketStatus.InProgress
            "resolved" -> TicketStatus.Resolved
            else -> TicketStatus.Open
        },
        createdAt = OffsetDateTime.parse(o.getString("created_at")).toInstant().toEpochMilli(),
        messages = messages,
        displayId = "T-" + o.optLong("number")
    )
}
