package com.nebula.app.core.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot"
    const val RESET = "reset"
    const val VERIFY = "verify"
    const val OAUTH = "oauth"

    const val HOME = "home"
    const val GAMES = "games"
    const val BOOKINGS = "bookings"
    const val SUPPORT = "support"
    const val PROFILE = "profile"

    const val GAME_DETAIL = "game/{gameId}"
    fun gameDetail(gameId: String) = "game/${Uri.encode(gameId)}"

    const val STREAM = "stream/{gameId}"
    fun stream(gameId: String) = "stream/${Uri.encode(gameId)}"

    const val BOOK_PLANS = "book/plans"
    const val BOOK_SESSION = "book/session/{optionId}?gameId={gameId}"
    fun bookSession(optionId: String, gameId: String? = null) =
        "book/session/${Uri.encode(optionId)}" + if (gameId != null) "?gameId=${Uri.encode(gameId)}" else ""

    const val BOOK_REVIEW = "book/review/{optionId}/{date}/{start}?gameId={gameId}"
    fun bookReview(optionId: String, date: String, startMinutes: Int, gameId: String? = null) =
        "book/review/${Uri.encode(optionId)}/$date/$startMinutes" +
            if (gameId != null) "?gameId=${Uri.encode(gameId)}" else ""

    const val BOOK_DONE = "book/done/{bookingId}"
    fun bookDone(bookingId: String) = "book/done/${Uri.encode(bookingId)}"

    const val BOOKING_DETAIL = "booking/{bookingId}"
    fun bookingDetail(bookingId: String) = "booking/${Uri.encode(bookingId)}"

    const val TICKET_NEW = "support/new"
    const val TICKET_DETAIL = "support/ticket/{ticketId}"
    fun ticketDetail(ticketId: String) = "support/ticket/${Uri.encode(ticketId)}"

    const val PROFILE_EDIT = "profile/edit"
    const val FAVOURITES = "profile/favourites"
    const val HISTORY = "profile/history"
    const val NOTIFICATIONS = "profile/notifications"
    const val ABOUT = "profile/about"
    const val PRIVACY = "legal/privacy"
    const val TERMS = "legal/terms"
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    TopLevelDestination(Routes.GAMES, "Games", Icons.Outlined.SportsEsports, Icons.Filled.SportsEsports),
    TopLevelDestination(Routes.BOOKINGS, "Bookings", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TopLevelDestination(Routes.SUPPORT, "Support", Icons.Outlined.SupportAgent, Icons.Filled.SupportAgent),
    TopLevelDestination(Routes.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person)
)
