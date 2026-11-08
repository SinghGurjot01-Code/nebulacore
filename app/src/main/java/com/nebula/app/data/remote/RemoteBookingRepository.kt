package com.nebula.app.data.remote

import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingOption
import com.nebula.app.domain.models.BookingStatus
import com.nebula.app.domain.models.TimeSlot
import com.nebula.app.domain.repositories.BookingRepository
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Bookings stored in Supabase. Capacity checks, overlap checks and the cancellation rule are
 * enforced on the server (create_booking / cancel_booking). The app only mirrors them for display.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteBookingRepository(
    private val client: SupabaseClient,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : BookingRepository {

    override val isSimulated: Boolean = false

    private val reload = MutableStateFlow(0)
    private var optionsCache: List<BookingOption>? = null
    private var capacityCache: Int? = null

    override val bookings: Flow<List<Booking>> = reload.flatMapLatest { flow { emit(fetchBookings()) } }

    override suspend fun getOptions(): List<BookingOption> {
        optionsCache?.let { return it }
        val response = client.request(
            "GET", "/rest/v1/booking_options?select=*&order=sort_order.asc", authenticated = false
        )
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load session options."))
        val array = JSONArray(response.body)
        val options = (0 until array.length()).map {
            val o = array.getJSONObject(it)
            BookingOption(o.getString("id"), o.getString("title"), o.getString("description"), o.getInt("duration_minutes"))
        }
        optionsCache = options
        return options
    }

    override fun availableDates(): List<LocalDate> {
        val today = LocalDate.now(clock.withZone(zone))
        return (0L until DAYS_AHEAD).map { today.plusDays(it) }
    }

    override suspend fun slotsFor(date: LocalDate, option: BookingOption): List<TimeSlot> {
        val dayStart = date.atStartOfDay(zone)
        val from = dayStart.toInstant()
        val to = dayStart.plusDays(2).toInstant()
        val response = client.request(
            "POST", "/rest/v1/rpc/get_booking_load",
            JSONObject().put("p_from", from.toString()).put("p_to", to.toString()).toString()
        )
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load available times."))
        val array = JSONArray(response.body)
        val busy = (0 until array.length()).map {
            val o = array.getJSONObject(it)
            OffsetDateTime.parse(o.getString("starts_at")).toInstant() to OffsetDateTime.parse(o.getString("ends_at")).toInstant()
        }
        val capacity = capacity()
        val now = clock.instant()
        return (FIRST_SLOT_HOUR..LAST_SLOT_HOUR step 2).map { hour ->
            val startMinutes = hour * 60
            val start = dayStart.plusMinutes(startMinutes.toLong()).toInstant()
            val end = start.plusSeconds(option.durationMinutes * 60L)
            val overlapping = busy.count { it.first < end && it.second > start }
            TimeSlot(startMinutes, startMinutes + option.durationMinutes, available = start.isAfter(now) && overlapping < capacity)
        }
    }

    override suspend fun createBooking(
        optionId: String,
        gameId: String?,
        gameTitle: String?,
        date: LocalDate,
        startMinutes: Int
    ): Result<Booking> = safeResult {
        val option = getOptions().firstOrNull { it.id == optionId }
            ?: throw NebulaException("That session option no longer exists.")
        val startsAt = date.atStartOfDay(zone).plusMinutes(startMinutes.toLong()).toOffsetDateTime()
        val body = JSONObject()
            .put("p_option_id", optionId)
            .put("p_game_id", gameId ?: JSONObject.NULL)
            .put("p_starts_at", startsAt.toString())
            .toString()
        val response = client.request("POST", "/rest/v1/rpc/create_booking", body)
        if (!response.ok) throw NebulaException(response.errorMessage("Could not create the booking."))
        reload.value = reload.value + 1
        Booking(
            id = response.json().getString("id"),
            optionId = option.id,
            optionTitle = option.title,
            gameId = gameId,
            gameTitle = gameTitle,
            date = date,
            startMinutes = startMinutes,
            durationMinutes = option.durationMinutes,
            status = BookingStatus.Upcoming
        )
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = safeResult {
        val response = client.request(
            "POST", "/rest/v1/rpc/cancel_booking",
            JSONObject().put("p_booking_id", bookingId).toString()
        )
        if (!response.ok) throw NebulaException(response.errorMessage("Could not cancel the booking."))
        reload.value = reload.value + 1
    }

    override suspend fun getBooking(id: String): Booking? = fetchBookings().firstOrNull { it.id == id }

    private suspend fun capacity(): Int {
        capacityCache?.let { return it }
        val response = client.request(
            "GET", "/rest/v1/app_settings?select=int_value&key=eq.host_capacity", authenticated = false
        )
        val value = if (response.ok) {
            val array = JSONArray(response.body)
            if (array.length() > 0) array.getJSONObject(0).getInt("int_value") else DEFAULT_CAPACITY
        } else {
            DEFAULT_CAPACITY
        }
        capacityCache = value
        return value
    }

    private suspend fun fetchBookings(): List<Booking> {
        val response = client.request(
            "GET",
            "/rest/v1/bookings?select=id,option_id,game_id,starts_at,duration_minutes,status," +
                "booking_options(title),games(title)&order=starts_at.asc"
        )
        if (response.code == 401) throw NebulaException("Please log in to see your bookings.")
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load your bookings."))
        val array = JSONArray(response.body)
        val now = clock.instant()
        return (0 until array.length()).map { index ->
            val o = array.getJSONObject(index)
            val start = OffsetDateTime.parse(o.getString("starts_at")).atZoneSameInstant(zone)
            val duration = o.getInt("duration_minutes")
            var status = when (o.getString("status")) {
                "cancelled" -> BookingStatus.Cancelled
                "completed" -> BookingStatus.Completed
                else -> BookingStatus.Upcoming
            }
            if (status == BookingStatus.Upcoming && start.toInstant().plusSeconds(duration * 60L).isBefore(now)) {
                status = BookingStatus.Completed
            }
            Booking(
                id = o.getString("id"),
                optionId = o.getString("option_id"),
                optionTitle = o.optJSONObject("booking_options")?.str("title") ?: o.getString("option_id"),
                gameId = o.str("game_id"),
                gameTitle = o.optJSONObject("games")?.str("title"),
                date = start.toLocalDate(),
                startMinutes = start.hour * 60 + start.minute,
                durationMinutes = duration,
                status = status
            )
        }
    }

    private companion object {
        const val DAYS_AHEAD = 7L
        const val FIRST_SLOT_HOUR = 10
        const val LAST_SLOT_HOUR = 22
        const val DEFAULT_CAPACITY = 3
    }
}
