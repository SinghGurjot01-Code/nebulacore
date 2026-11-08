package com.nebula.app.domain.models

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val emailVerified: Boolean
) {
    val initials: String
        get() = fullName.trim().split(" ").filter { it.isNotBlank() }
            .take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "N" }
}

enum class GameAvailability(val label: String) {
    Available("Available"),
    HighDemand("High demand"),
    ComingSoon("Coming soon")
}

data class Game(
    val id: String,
    val title: String,
    val studio: String,
    val genre: String,
    val description: String,
    val coverUrl: String,
    val heroUrl: String,
    val availability: GameAvailability,
    val popular: Boolean = false,
    val featured: Boolean = false
) {
    /** Games that are not released can be browsed but not booked or launched. */
    val canPlay: Boolean get() = availability != GameAvailability.ComingSoon
}

data class BookingOption(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int
)

enum class BookingStatus(val label: String) {
    Upcoming("Upcoming"),
    Completed("Completed"),
    Cancelled("Cancelled")
}

data class TimeSlot(
    val startMinutes: Int,
    val endMinutes: Int,
    val available: Boolean
) {
    val label: String get() = "${formatMinutes(startMinutes)} - ${formatMinutes(endMinutes)}"
    val startLabel: String get() = formatMinutes(startMinutes)
}

fun formatMinutes(total: Int): String {
    val minutes = ((total % (24 * 60)) + 24 * 60) % (24 * 60)
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}

/** A simulated booking. No server reservation exists for it in this phase. */
data class Booking(
    val id: String,
    val optionId: String,
    val optionTitle: String,
    val gameId: String?,
    val gameTitle: String?,
    val date: LocalDate,
    val startMinutes: Int,
    val durationMinutes: Int,
    val status: BookingStatus
) {
    val endMinutes: Int get() = startMinutes + durationMinutes
    val start: LocalDateTime get() = date.atStartOfDay().plusMinutes(startMinutes.toLong())
    val end: LocalDateTime get() = start.plusMinutes(durationMinutes.toLong())
    val timeLabel: String get() = "${formatMinutes(startMinutes)} - ${formatMinutes(endMinutes)}"

    fun isCancellable(clock: Clock): Boolean =
        status == BookingStatus.Upcoming &&
            !start.isBefore(LocalDateTime.now(clock).plusMinutes(MIN_CANCEL_NOTICE_MINUTES))

    companion object {
        const val MIN_CANCEL_NOTICE_MINUTES = 60L
    }
}

data class GamingSession(
    val id: String,
    val gameId: String,
    val gameTitle: String,
    val playedOn: LocalDate,
    val durationMinutes: Int
)

enum class TicketStatus(val label: String) {
    Open("Open"),
    InProgress("In progress"),
    Resolved("Resolved")
}

enum class MessageAuthor { User, Support }

data class TicketMessage(
    val id: String,
    val author: MessageAuthor,
    val text: String,
    val timestamp: Long
)

/** Local demo ticket. No support team receives it in this phase. */
data class SupportTicket(
    val id: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val createdAt: Long,
    val messages: List<TicketMessage>,
    val displayId: String = id
)

data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val read: Boolean
)

data class Announcement(
    val id: String,
    val title: String,
    val body: String
)

data class NotificationPreferences(
    val bookingReminders: Boolean = true,
    val supportUpdates: Boolean = true,
    val announcements: Boolean = true
)

/** Maps a time of day (hh:mm) to minutes since midnight. */
fun LocalTime.toMinutes(): Int = hour * 60 + minute
