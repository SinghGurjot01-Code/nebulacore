package com.nebula.app.core.common

import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

fun LocalDate.pretty(): String =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()).format(this)

fun LocalDate.short(): String =
    DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(this)

fun LocalDate.relativeTo(today: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(this, today)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days in 2..30 -> "$days days ago"
        else -> pretty()
    }
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> if (h == 1) "1 hour" else "$h hours"
        else -> "${h}h ${m}m"
    }
}

fun formatTimestamp(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(millis))
