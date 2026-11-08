package com.nebula.app.core.navigation

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.nebula.app.features.auth.ForgotPasswordScreen
import com.nebula.app.features.auth.LoginScreen
import com.nebula.app.features.auth.RegisterScreen
import com.nebula.app.features.auth.ResetPasswordScreen
import com.nebula.app.features.auth.SplashScreen
import com.nebula.app.features.auth.VerifyEmailScreen
import com.nebula.app.features.auth.WelcomeScreen
import com.nebula.app.features.bookings.BookingConfirmedScreen
import com.nebula.app.features.bookings.BookingDetailScreen
import com.nebula.app.features.bookings.BookingPlansScreen
import com.nebula.app.features.bookings.BookingReviewScreen
import com.nebula.app.features.bookings.BookingSessionScreen
import com.nebula.app.features.bookings.BookingsScreen
import com.nebula.app.features.games.GameDetailScreen
import com.nebula.app.features.games.GamesScreen
import com.nebula.app.features.home.HomeScreen
import com.nebula.app.features.profile.AboutScreen
import com.nebula.app.features.profile.EditProfileScreen
import com.nebula.app.features.profile.FavouritesScreen
import com.nebula.app.features.profile.HistoryScreen
import com.nebula.app.features.profile.LegalScreen
import com.nebula.app.features.profile.NotificationsScreen
import com.nebula.app.features.profile.ProfileScreen
import com.nebula.app.features.streaming.StreamingScreen
import com.nebula.app.features.support.CreateTicketScreen
import com.nebula.app.features.support.SupportScreen
import com.nebula.app.features.support.TicketDetailScreen
import java.time.LocalDate

/** The nebula:// link that opened this destination, if any (it carries email tokens in the fragment). */
@Suppress("DEPRECATION")
private fun NavBackStackEntry.deepLinkUri(): String? =
    arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)?.data?.toString()

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun NebulaNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(140)) }
    ) {
        // ---------------------------------------------------- Authentication
        composable(Routes.SPLASH) {
            // An email link may already have opened another screen; only leave splash if it is still shown.
            SplashScreen(
                onSignedIn = { if (navController.currentDestination?.route == Routes.SPLASH) navController.resetTo(Routes.HOME) },
                onSignedOut = { if (navController.currentDestination?.route == Routes.SPLASH) navController.resetTo(Routes.WELCOME) }
            )
        }
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = back,
                onLoggedIn = { navController.resetTo(Routes.HOME) },
                onForgotPassword = { navController.navigate(Routes.FORGOT) },
                onRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = back,
                onRegistered = { navController.resetTo(Routes.VERIFY) },
                onLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                onBack = back,
                onContinueToReset = { navController.navigate(Routes.RESET) }
            )
        }
        composable(
            Routes.RESET,
            deepLinks = listOf(navDeepLink { uriPattern = "nebula://auth/reset" })
        ) { entry ->
            ResetPasswordScreen(
                onBack = back,
                onDone = { navController.resetTo(Routes.LOGIN) },
                linkUri = entry.deepLinkUri()
            )
        }
        composable(
            Routes.VERIFY,
            deepLinks = listOf(navDeepLink { uriPattern = "nebula://auth/verified" })
        ) { entry ->
            VerifyEmailScreen(
                onContinue = { navController.resetTo(Routes.HOME) },
                onLogin = { navController.resetTo(Routes.LOGIN) },
                linkUri = entry.deepLinkUri()
            )
        }

        // ---------------------------------------------------- Main tabs
        composable(Routes.HOME) {
            HomeScreen(
                innerPadding = innerPadding,
                onOpenGame = { navController.navigate(Routes.gameDetail(it)) },
                onBrowseGames = { navController.openTab(Routes.GAMES) },
                onOpenBooking = { navController.navigate(Routes.bookingDetail(it)) },
                onBookSession = { navController.navigate(Routes.BOOK_PLANS) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenProfile = { navController.openTab(Routes.PROFILE) }
            )
        }
        composable(Routes.GAMES) {
            GamesScreen(
                innerPadding = innerPadding,
                onOpenGame = { navController.navigate(Routes.gameDetail(it)) }
            )
        }
        composable(Routes.BOOKINGS) {
            BookingsScreen(
                innerPadding = innerPadding,
                onNewBooking = { navController.navigate(Routes.BOOK_PLANS) },
                onOpenBooking = { navController.navigate(Routes.bookingDetail(it)) }
            )
        }
        composable(Routes.SUPPORT) {
            SupportScreen(
                innerPadding = innerPadding,
                onNewTicket = { navController.navigate(Routes.TICKET_NEW) },
                onOpenTicket = { navController.navigate(Routes.ticketDetail(it)) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                innerPadding = innerPadding,
                onEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                onFavourites = { navController.navigate(Routes.FAVOURITES) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onTerms = { navController.navigate(Routes.TERMS) },
                onVerifyEmail = { navController.navigate(Routes.VERIFY) },
                onLoggedOut = { navController.resetTo(Routes.WELCOME) }
            )
        }

        // ---------------------------------------------------- Games and streaming
        composable(
            Routes.GAME_DETAIL,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { entry ->
            val gameId = entry.arguments?.getString("gameId").orEmpty()
            GameDetailScreen(
                gameId = gameId,
                onBack = back,
                onPlay = { navController.navigate(Routes.stream(gameId)) },
                onBook = { navController.navigate(Routes.bookSession(optionId = "standard", gameId = gameId)) }
            )
        }
        composable(
            Routes.STREAM,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { entry ->
            val gameId = entry.arguments?.getString("gameId").orEmpty()
            StreamingScreen(
                gameId = gameId,
                onExit = back,
                onBook = { navController.navigate(Routes.bookSession(optionId = "standard", gameId = gameId)) }
            )
        }

        // ---------------------------------------------------- Bookings
        composable(Routes.BOOK_PLANS) {
            BookingPlansScreen(
                onBack = back,
                onSelect = { navController.navigate(Routes.bookSession(it)) }
            )
        }
        composable(
            Routes.BOOK_SESSION,
            arguments = listOf(
                navArgument("optionId") { type = NavType.StringType },
                navArgument("gameId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { entry ->
            val optionId = entry.arguments?.getString("optionId").orEmpty()
            val gameId = entry.arguments?.getString("gameId")
            BookingSessionScreen(
                optionId = optionId,
                initialGameId = gameId,
                onBack = back,
                onReview = { date, start, selectedGameId ->
                    navController.navigate(Routes.bookReview(optionId, date.toString(), start, selectedGameId))
                }
            )
        }
        composable(
            Routes.BOOK_REVIEW,
            arguments = listOf(
                navArgument("optionId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("start") { type = NavType.IntType },
                navArgument("gameId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { entry ->
            val optionId = entry.arguments?.getString("optionId").orEmpty()
            val date = LocalDate.parse(entry.arguments?.getString("date").orEmpty())
            val start = entry.arguments?.getInt("start") ?: 0
            val gameId = entry.arguments?.getString("gameId")
            BookingReviewScreen(
                optionId = optionId,
                date = date,
                startMinutes = start,
                gameId = gameId,
                onBack = back,
                onConfirmed = { id ->
                    navController.navigate(Routes.bookDone(id)) {
                        popUpTo(Routes.BOOK_PLANS) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.BOOK_DONE,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("bookingId").orEmpty()
            BookingConfirmedScreen(
                bookingId = id,
                onViewBooking = {
                    navController.navigate(Routes.bookingDetail(id)) {
                        popUpTo(Routes.BOOK_DONE) { inclusive = true }
                    }
                },
                onDone = { navController.openTab(Routes.BOOKINGS) }
            )
        }
        composable(
            Routes.BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { entry ->
            BookingDetailScreen(
                bookingId = entry.arguments?.getString("bookingId").orEmpty(),
                onBack = back,
                onOpenGame = { navController.navigate(Routes.gameDetail(it)) }
            )
        }

        // ---------------------------------------------------- Support
        composable(Routes.TICKET_NEW) {
            CreateTicketScreen(
                onBack = back,
                onCreated = { id ->
                    navController.navigate(Routes.ticketDetail(id)) {
                        popUpTo(Routes.TICKET_NEW) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.TICKET_DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { entry ->
            TicketDetailScreen(
                ticketId = entry.arguments?.getString("ticketId").orEmpty(),
                onBack = back
            )
        }

        // ---------------------------------------------------- Profile
        composable(Routes.PROFILE_EDIT) { EditProfileScreen(onBack = back) }
        composable(Routes.FAVOURITES) {
            FavouritesScreen(
                onBack = back,
                onOpenGame = { navController.navigate(Routes.gameDetail(it)) }
            )
        }
        composable(Routes.HISTORY) { HistoryScreen(onBack = back) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = back) }
        composable(Routes.ABOUT) { AboutScreen(onBack = back) }
        composable(Routes.PRIVACY) {
            LegalScreen(
                title = "Privacy policy",
                body = "This is a placeholder. The final privacy policy will describe what data Nebula collects, how it is used and how you can request deletion. " +
                    "In this demo build all data stays on your device.",
                onBack = back
            )
        }
        composable(Routes.TERMS) {
            LegalScreen(
                title = "Terms and conditions",
                body = "This is a placeholder. The final terms will cover acceptable use, bookings, cancellations and the cloud gaming service. " +
                    "Nothing in this demo build creates a real reservation or contract.",
                onBack = back
            )
        }
    }
}
