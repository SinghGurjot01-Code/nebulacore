package com.nebula.app.features.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.domain.Validation
import com.nebula.app.domain.models.SupportTicket
import com.nebula.app.domain.models.TicketStatus
import com.nebula.app.domain.repositories.SupportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupportViewModel(repository: SupportRepository) : ViewModel() {
    val state: StateFlow<Loadable<List<SupportTicket>>> = repository.tickets
        .map<List<SupportTicket>, Loadable<List<SupportTicket>>> { Loadable.Ready(it) }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)
}

data class CreateTicketUiState(
    val category: String,
    val subject: String = "",
    val description: String = "",
    val subjectError: String? = null,
    val descriptionError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false
)

class CreateTicketViewModel(private val repository: SupportRepository) : ViewModel() {
    val categories: List<String> = repository.categories

    private val _state = MutableStateFlow(CreateTicketUiState(category = categories.first()))
    val state: StateFlow<CreateTicketUiState> = _state.asStateFlow()

    fun onCategory(value: String) = _state.update { it.copy(category = value) }
    fun onSubject(value: String) = _state.update { it.copy(subject = value, subjectError = null, formError = null) }
    fun onDescription(value: String) = _state.update { it.copy(description = value, descriptionError = null, formError = null) }

    fun submit(onCreated: (String) -> Unit) {
        val s = _state.value
        if (s.loading) return
        val subjectError = Validation.required(s.subject, "Subject", minLength = 4)
        val descriptionError = Validation.required(s.description, "Description", minLength = 10)
        if (subjectError != null || descriptionError != null) {
            _state.update { it.copy(subjectError = subjectError, descriptionError = descriptionError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, formError = null) }
            repository.createTicket(s.category, s.subject, s.description)
                .onSuccess { ticket ->
                    _state.update { it.copy(loading = false) }
                    onCreated(ticket.id)
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.userMessage()) }
                }
        }
    }
}

class TicketDetailViewModel(
    private val repository: SupportRepository,
    private val ticketId: String
) : ViewModel() {

    val state: StateFlow<Loadable<SupportTicket>> = repository.ticket(ticketId)
        .map<SupportTicket?, Loadable<SupportTicket>> { ticket ->
            if (ticket == null) Loadable.Failed("Ticket not found.") else Loadable.Ready(ticket)
        }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun onDraftChange(value: String) { _draft.value = value }

    fun send(onError: (String) -> Unit) {
        val text = _draft.value
        if (text.isBlank() || _sending.value) return
        viewModelScope.launch {
            _sending.value = true
            repository.addReply(ticketId, text)
                .onSuccess { _draft.value = "" }
                .onFailure { onError(it.userMessage()) }
            _sending.value = false
        }
    }
}

fun TicketStatus.isActive(): Boolean = this != TicketStatus.Resolved
