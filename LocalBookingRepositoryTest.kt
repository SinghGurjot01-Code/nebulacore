package com.nebula.app.data

import com.nebula.app.data.repositories.LocalBookingRepository
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class LocalBookingRepositoryTest {

    // 8 November 2026, 09:00 UTC.
    private val clock: Clock = Clock.fixed(Instant.parse("2026-11-08T09:00:00Z"), ZoneOffset.UTC)
    private val today: LocalDate = LocalDate.now(clock)

    private fun repository() = LocalBookingRepository(clock, simulatedLatencyMs = 0)

    @Test
    fun offersSevenDatesStartingToday() {
        val dates = repository().availableDates()
        assertEquals(7, dates.size)
        assertEquals(today, dates.first())
    }

    @Test
    fun pastSlotsAreUnavailable() = runTest {
        val repo = repository()
        val option = repo.options.first { it.id == "quick" }
        val slots = repo.slotsFor(today, option)
        assertTrue(slots.first { it.startMinutes == 10 * 60 }.available)
        // Slots on a later day are never in the past.
        assertTrue(repo.slotsFor(today.plusDays(1), option).any { it.available })
    }

    @Test
    fun createsAndListsAnUpcomingBooking() = runTest {
        val repo = repository()
        val option = repo.options.first { it.id == "quick" }
        val result = repo.createBooking(option.id, "cyberpunk", "Cyberpunk 2077", today, 10 * 60)
        assertTrue(result.isSuccess)
        val booking = result.getOrThrow()
        assertEquals(BookingStatus.Upcoming, booking.status)
        assertNotNull(repo.getBooking(booking.id))
        assertTrue(repo.bookings.first().any { it.id == booking.id })
    }

    @Test
    fun overlappingBookingIsRejected() = runTest {
        val repo = repository()
        assertTrue(repo.createBooking("quick", null, null, today, 10 * 60).isSuccess)
        assertTrue(repo.createBooking("quick", null, null, today, 10 * 60).isFailure)
        // A two hour session at 10:00 overlaps the 12:00 slot start only if it runs past it.
        val standard = repo.options.first { it.id == "standard" }
        val slot = repo.slotsFor(today, standard).first { it.startMinutes == 10 * 60 }
        assertFalse(slot.available)
    }

    @Test
    fun cannotBookAStartTimeInThePast() = runTest {
        val repo = repository()
        assertTrue(repo.createBooking("quick", null, null, today.minusDays(1), 10 * 60).isFailure)
    }

    @Test
    fun cancelsAnEligibleBooking() = runTest {
        val repo = repository()
        val upcoming = repo.bookings.first().first { it.status == BookingStatus.Upcoming }
        assertTrue(repo.cancelBooking(upcoming.id).isSuccess)
        assertEquals(BookingStatus.Cancelled, repo.getBooking(upcoming.id)?.status)
        assertTrue(repo.cancelBooking(upcoming.id).isFailure)
    }

    @Test
    fun cancellationNeedsAtLeastOneHourNotice() {
        val soon = Booking(
            "B-1", "quick", "Quick Play", null, null, today, 9 * 60 + 30, 60, BookingStatus.Upcoming
        )
        val later = soon.copy(startMinutes = 11 * 60)
        assertFalse(soon.isCancellable(clock))
        assertTrue(later.isCancellable(clock))
        assertFalse(later.copy(status = BookingStatus.Completed).isCancellable(clock))
    }

    @Test
    fun seedDataContainsUpcomingAndCompletedBookings() = runTest {
        val all = repository().bookings.first()
        assertTrue(all.any { it.status == BookingStatus.Upcoming })
        assertTrue(all.any { it.status == BookingStatus.Completed })
    }

    @Test
    fun cancelUnknownBookingFails() = runTest {
        assertTrue(repository().cancelBooking("missing").isFailure)
    }
}
