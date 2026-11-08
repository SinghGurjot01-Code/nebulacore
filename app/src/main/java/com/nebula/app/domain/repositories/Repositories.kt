package com.nebula.app.domain.repositories

import com.nebula.app.domain.models.Announcement
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingOption
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GamingSession
import com.nebula.app.domain.models.Notification
import com.nebula.app.domain.models.NotificationPreferences
import com.nebula.app.domain.models.PreflightCheck
import com.nebula.app.domain.models.StreamSession
import com.nebula.app.domain.models.StreamingState
import com.nebula.app.domain.models.SupportTicket
import com.nebula.app.domain.models.TimeSlot
import com.nebula.app.domain.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/** User-facing failure. The message is safe to show in the UI. */
class NebulaException(message: String) : Exception(message)

interface AuthRepository {
    /** Emits the signed-in user, or null when signed out. */
    val session: Flow<User?>

    /** True for the local on-device implementation, false when backed by Supabase. */
    val isDemo: Boolean get() = true

    /** Email waiting for confirmation after sign-up, when no session exists yet. */
    val pendingVerificationEmail: Flow<String?> get() = flowOf(null)

    /** Google sign-in needs backend configuration and is not available yet. */
    val googleSignInAvailable: Boolean

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, password: String): Result<User>
    suspend fun requestPasswordReset(email: String): Result<Unit>
    suspend fun resetPassword(newPassword: String): Result<Unit>
    suspend fun markEmailVerified(): Result<Unit>
    suspend fun updateFullName(fullName: String): Result<Unit>
    suspend fun logout()

    suspend fun resendVerification(): Result<Unit> =
        Result.failure(NebulaException("Not available in demo mode."))

    /** Handles a nebula://auth/... link from an email. Returns true when it was accepted. */
    suspend fun handleAuthLink(uri: String): Boolean = false
}

interface GameRepository {
    val games: Flow<List<Game>>
    val favouriteIds: Flow<Set<String>>
    val recentSessions: Flow<List<GamingSession>>
    suspend fun getGame(id: String): Game?
    suspend fun toggleFavourite(gameId: String)
    suspend fun announcements(): List<Announcement>
}

interface BookingRepository {
    /** True when bookings exist only on this device. */
    val isSimulated: Boolean get() = true

    suspend fun getOptions(): List<BookingOption>
    val bookings: Flow<List<Booking>>
    fun availableDates(): List<LocalDate>
    suspend fun slotsFor(date: LocalDate, option: BookingOption): List<TimeSlot>
    suspend fun createBooking(
        optionId: String,
        gameId: String?,
        gameTitle: String?,
        date: LocalDate,
        startMinutes: Int
    ): Result<Booking>
    suspend fun cancelBooking(bookingId: String): Result<Unit>
    suspend fun getBooking(id: String): Booking?
}

interface SupportRepository {
    /** True when tickets exist only on this device. */
    val isSimulated: Boolean get() = true

    val categories: List<String>
    val tickets: Flow<List<SupportTicket>>
    fun ticket(id: String): Flow<SupportTicket?>
    suspend fun createTicket(category: String, subject: String, description: String): Result<SupportTicket>
    suspend fun addReply(ticketId: String, text: String): Result<Unit>
}

interface ProfileRepository {
    val notificationPreferences: Flow<NotificationPreferences>
    val notifications: Flow<List<Notification>>
    suspend fun setBookingReminders(enabled: Boolean)
    suspend fun setSupportUpdates(enabled: Boolean)
    suspend fun setAnnouncements(enabled: Boolean)
    suspend fun markAllNotificationsRead()
}

/**
 * Integration boundary for a Moonlight-compatible streaming client.
 * The first phase only ships [com.nebula.app.data.repositories.UnavailableStreamingClient].
 */
interface StreamingClient {
    val state: StateFlow<StreamingState>
    suspend fun start(session: StreamSession)
    fun stop()
}

interface StreamingRepository {
    val client: StreamingClient
    suspend fun runPreflight(game: Game): List<PreflightCheck>
}
