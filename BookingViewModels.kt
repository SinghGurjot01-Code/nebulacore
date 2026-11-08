package com.nebula.app.features.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.loadCatching
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingOption
import com.nebula.app.domain.models.BookingStatus
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.TimeSlot
import com.nebula.app.domain.repositories.BookingRepository
import com.nebula.app.domain.repositories.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

// ------------------------------------------------------------------ Overview

data class BookingsContent(val upcoming: List<Booking>, val history: List<Booking>)

class BookingsViewModel(repository: BookingRepository) : ViewModel() {
    val state: StateFlow<Loadable<BookingsContent>> = repository.bookings
        .map<List<Booking>, Loadable<BookingsContent>> { all ->
            Loadable.Ready(
                BookingsContent(
                    upcoming = all.filter { it.status == BookingStatus.Upcoming }.sortedBy { it.start },
                    history = all.filter { it.status != BookingStatus.Upcoming }.sortedByDescending { it.start }
                )
            )
        }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)
}

class BookingPlansViewModel(private val repository: BookingRepository) : ViewModel() {
    private val _state = MutableStateFlow<Loadable<List<BookingOption>>>(Loadable.Loading)
    val state: StateFlow<Loadable<List<BookingOption>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Loadable.Loading
            _state.value = loadCatching { repository.getOptions() }
        }
    }
}

// ------------------------------------------------------------------ Choose date and time

data class SessionUiState(
    val loading: Boolean = true,
    val slotsLoading: Boolean = false,
    val error: String? = null,
    val option: BookingOption? = null,
    val dates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val slots: List<TimeSlot> = emptyList(),
    val selectedStart: Int? = null,
    val games: List<Game> = emptyList(),
    val selectedGame: Game? = null
)

class BookingSessionViewModel(
    private val bookings: BookingRepository,
    private val games: GameRepository,
    private val optionId: String,
    private val initialGameId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val option = bookings.getOptions().firstOrNull { it.id == optionId }
                val dates = bookings.availableDates()
                val first = dates.first()
                val playable = games.games.first().filter { it.canPlay }
                val slots = if (option != null) bookings.slotsFor(first, option) else emptyList()
                _state.update {
                    it.copy(
                        loading = false,
                        option = option,
                        dates = dates,
                        selectedDate = first,
                        slots = slots,
                        selectedStart = null,
                        games = playable,
                        selectedGame = playable.firstOrNull { g -> g.id == initialGameId }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val option = _state.value.option ?: return
        viewModelScope.launch {
            _state.update { it.copy(selectedDate = date, selectedStart = null, slotsLoading = true) }
            try {
                val slots = bookings.slotsFor(date, option)
                _state.update { it.copy(slots = slots, slotsLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(slots = emptyList(), slotsLoading = false, error = e.userMessage()) }
            }
        }
    }

    fun selectSlot(startMinutes: Int) = _state.update { it.copy(selectedStart = startMinutes) }

    fun selectGame(game: Game?) = _state.update { it.copy(selectedGame = game) }
}

// ------------------------------------------------------------------ Review and confirm

data class ReviewUiState(
    val loading: Boolean = true,
    val option: BookingOption? = null,
    val game: Game? = null,
    val submitting: Boolean = false,
    val error: String? = null
)

class BookingReviewViewModel(
    private val bookings: BookingRepository,
    private val games: GameRepository,
    private val optionId: String,
    private val date: LocalDate,
    private val startMinutes: Int,
    private val gameId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val option = bookings.getOptions().firstOrNull { it.id == optionId }
                val game = if (gameId != null) games.getGame(gameId) else null
                _state.update { it.copy(loading = false, option = option, game = game) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.userMessage()) }
            }
        }
    }

    fun confirm(onConfirmed: (String) -> Unit) {
        if (_state.value.submitting || _state.value.option == null) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val game = _state.value.game
            bookings.createBooking(optionId, game?.id, game?.title, date, startMinutes)
                .onSuccess { booking ->
                    _state.update { it.copy(submitting = false) }
                    onConfirmed(booking.id)
                }
                .onFailure { error ->
                    _state.update { it.copy(submitting = false, error = error.userMessage()) }
                }
        }
    }
}

// ------------------------------------------------------------------ Confirmed / details

class BookingConfirmedViewModel(private val repository: BookingRepository, private val id: String) : ViewModel() {
    private val _booking = MutableStateFlow<Loadable<Booking>>(Loadable.Loading)
    val booking: StateFlow<Loadable<Booking>> = _booking.asStateFlow()

    init {
        viewModelScope.launch {
            val found = repository.getBooking(id)
            _booking.value = if (found != null) Loadable.Ready(found) else Loadable.Failed("Booking not found.")
        }
    }
}

data class BookingDetail(val booking: Booking, val cancellable: Boolean)

class BookingDetailViewModel(
    private val repository: BookingRepository,
    private val id: String,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    val state: StateFlow<Loadable<BookingDetail>> = repository.bookings
        .map<List<Booking>, Loadable<BookingDetail>> { list ->
            val booking = list.firstOrNull { it.id == id }
            if (booking == null) Loadable.Failed("Booking not found.")
            else Loadable.Ready(BookingDetail(booking, booking.isCancellable(clock)))
        }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    private val _cancelling = MutableStateFlow(false)
    val cancelling: StateFlow<Boolean> = _cancelling.asStateFlow()

    fun cancel(onResult: (Result<Unit>) -> Unit) {
        if (_cancelling.value) return
        viewModelScope.launch {
            _cancelling.value = true
            val result = repository.cancelBooking(id)
            _cancelling.value = false
            onResult(result)
        }
    }
}
