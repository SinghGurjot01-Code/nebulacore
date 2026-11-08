package com.nebula.app.data.local

import com.nebula.app.domain.models.Announcement
import com.nebula.app.domain.models.BookingOption
import com.nebula.app.domain.models.Notification

object DemoContent {

    val bookingOptions = listOf(
        BookingOption(
            id = "quick",
            title = "Quick Play",
            description = "A short session to try a game or finish a quest.",
            durationMinutes = 60
        ),
        BookingOption(
            id = "standard",
            title = "Standard Session",
            description = "Two hours, a good fit for most story games.",
            durationMinutes = 120
        ),
        BookingOption(
            id = "extended",
            title = "Extended Session",
            description = "Four hours for long play sessions and open-world games.",
            durationMinutes = 240
        ),
        BookingOption(
            id = "marathon",
            title = "Marathon",
            description = "Six hours for weekends and big releases.",
            durationMinutes = 360
        )
    )

    val announcements = listOf(
        Announcement(
            id = "a1",
            title = "Nebula is in development",
            body = "You are using the frontend preview. Cloud streaming and real bookings arrive in a later phase."
        ),
        Announcement(
            id = "a2",
            title = "New games added to the preview catalogue",
            body = "Browse the Games tab to see the titles planned for launch."
        ),
        Announcement(
            id = "a3",
            title = "Share your feedback",
            body = "Open a support ticket from the Support tab to tell us what you think of the app."
        )
    )

    val notifications = listOf(
        Notification("n1", "Welcome to Nebula", "Explore the catalogue and try a demo booking.", "Today", false),
        Notification("n2", "Booking reminder", "Your upcoming demo booking is coming up soon.", "Yesterday", false),
        Notification("n3", "Support update", "A demo support ticket has a new reply.", "2 days ago", true),
        Notification("n4", "Catalogue update", "New titles were added to the preview catalogue.", "Last week", true)
    )

    val supportCategories = listOf(
        "Technical issue",
        "Bookings",
        "Account",
        "Game request",
        "Other"
    )
}
