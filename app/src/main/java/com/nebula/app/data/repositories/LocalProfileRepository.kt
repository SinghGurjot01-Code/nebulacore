package com.nebula.app.data.repositories

import com.nebula.app.data.local.DemoContent
import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.domain.models.Notification
import com.nebula.app.domain.models.NotificationPreferences
import com.nebula.app.domain.repositories.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class LocalProfileRepository(private val store: KeyValueStore) : ProfileRepository {

    override val notificationPreferences: Flow<NotificationPreferences> =
        combine(
            store.observe(KEY_BOOKINGS),
            store.observe(KEY_SUPPORT),
            store.observe(KEY_ANNOUNCEMENTS)
        ) { bookings, support, announcements ->
            NotificationPreferences(
                bookingReminders = bookings != "false",
                supportUpdates = support != "false",
                announcements = announcements != "false"
            )
        }

    private val inbox = MutableStateFlow(DemoContent.notifications)

    override val notifications: Flow<List<Notification>> = inbox

    override suspend fun setBookingReminders(enabled: Boolean) = store.put(KEY_BOOKINGS, enabled.toString())
    override suspend fun setSupportUpdates(enabled: Boolean) = store.put(KEY_SUPPORT, enabled.toString())
    override suspend fun setAnnouncements(enabled: Boolean) = store.put(KEY_ANNOUNCEMENTS, enabled.toString())

    override suspend fun markAllNotificationsRead() {
        inbox.value = inbox.value.map { it.copy(read = true) }
    }

    private companion object {
        const val KEY_BOOKINGS = "pref_booking_reminders"
        const val KEY_SUPPORT = "pref_support_updates"
        const val KEY_ANNOUNCEMENTS = "pref_announcements"
    }
}
