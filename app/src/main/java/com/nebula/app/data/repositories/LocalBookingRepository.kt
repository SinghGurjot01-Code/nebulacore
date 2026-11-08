package com.nebula.app.data.repositories

import com.nebula.app.data.local.DemoContent
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingOption
import com.nebula.app.domain.models.BookingStatus
import com.nebula.app.domain.models.TimeSlot
import com.nebula.app.domain.models.toMinutes
import com.nebula.app.domain.repositories.BookingRepository
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Local simulated bookings. Availability is generated from the date, not from
 * a server, and confirmed bookings are held in memory only.
 */
class LocalBookingRepository(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val simulatedLatencyMs: Long = 500L
) : BookingRepository {

    val options: List<BookingOption> = DemoContent.bookingOptions

    override suspend fun getOptions(): List<BookingOption> = options

    private var nextId = 1000
    private val store = MutableStateFlow(seed())

    override val bookings: Flow<List<Booking>> = store.map { list -> list.map(::normalise) }

    override fun availableDates(): List<LocalDate> {
        val today = LocalDate.now(clock)
        return (0L until DAYS_AHEAD).map { today.plusDays(it) }
    }

    override suspend fun slotsFor(date: LocalDate, option: BookingOption): List<TimeSlot> {
        val now = LocalDateTime.now(clock)
        val existing = store.value
            .map(::normalise)
            .filter { it.status == BookingStatus.Upcoming && it.date == date }
        return FIRST_SLOT_HOUR.rangeTo(LAST_SLOT_HOUR).step(2).map { hour ->
            val start = hour * 60
            val end = start + option.durationMinutes
            val inFuture = date.atStartOfDay().plusMinutes(start.toLong()).isAfter(now)
            val demoBusy = (date.dayOfYear + hour / 2) % 5 == 0
            val clashes = existing.any { start < it.endMinutes && it.startMinutes < end }
            TimeSlot(start, end, available = inFuture && !demoBusy && !clashes)
        }
    }

    override suspend fun createBooking(
        optionId: String,
        gameId: String?,
        gameTitle: String?,
        date: LocalDate,
        startMinutes: Int
    ): Result<Booking> {
        delay(simulatedLatencyMs)
        val option = options.firstOrNull { it.id == optionId }
            ?: return Result.failure(NebulaException("That session option no longer exists."))
        val slot = slotsFor(date, option).firstOrNull { it.startMinutes == startMinutes }
        if (slot == null || !slot.available) {
            return Result.failure(NebulaException("That time slot is no longer available. Please pick another."))
        }
        val booking = Booking(
            id = "B-${nextId++}",
            optionId = option.id,
            optionTitle = option.title,
            gameId = gameId,
            gameTitle = gameTitle,
            date = date,
            startMinutes = startMinutes,
            durationMinutes = option.durationMinutes,
            status = BookingStatus.Upcoming
        )
        store.value = store.value + booking
        return Result.success(booking)
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> {
        delay(simulatedLatencyMs)
        val booking = store.value.firstOrNull { it.id == bookingId }?.let(::normalise)
            ?: return Result.failure(NebulaException("Booking not found."))
        if (!booking.isCancellable(clock)) {
            return Result.failure(
                NebulaException("Bookings can only be cancelled up to ${Booking.MIN_CANCEL_NOTICE_MINUTES} minutes before they start.")
            )
        }
        store.value = store.value.map {
            if (it.id == bookingId) it.copy(status = BookingStatus.Cancelled) else it
        }
        return Result.success(Unit)
    }

    override suspend fun getBooking(id: String): Booking? =
        store.value.firstOrNull { it.id == id }?.let(::normalise)

    /** An upcoming booking whose end time has passed counts as completed. */
    private fun normalise(booking: Booking): Booking =
        if (booking.status == BookingStatus.Upcoming && booking.end.isBefore(LocalDateTime.now(clock))) {
            booking.copy(status = BookingStatus.Completed)
        } else {
            booking
        }

    private fun seed(): List<Booking> {
        val today = LocalDate.now(clock)
        return listOf(
            Booking("B-901", "standard", "Standard Session", "cyberpunk", "Cyberpunk 2077",
                today.plusDays(2), LocalTime.of(20, 0).toMinutes(), 120, BookingStatus.Upcoming),
            Booking("B-900", "quick", "Quick Play", "god-of-war", "God of War",
                today.minusDays(3), LocalTime.of(18, 0).toMinutes(), 60, BookingStatus.Completed),
            Booking("B-899", "extended", "Extended Session", "elden", "Elden Ring",
                today.minusDays(9), LocalTime.of(14, 0).toMinutes(), 240, BookingStatus.Completed)
        )
    }

    companion object {
        const val DAYS_AHEAD = 7L
        const val FIRST_SLOT_HOUR = 10
        const val LAST_SLOT_HOUR = 22
    }
}
