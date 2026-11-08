package com.nebula.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.domain.models.Announcement
import com.nebula.app.domain.models.Booking
import com.nebula.app.domain.models.BookingStatus
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GamingSession
import com.nebula.app.domain.models.StreamingState
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.AuthRepository
import com.nebula.app.domain.repositories.BookingRepository
import com.nebula.app.domain.repositories.GameRepository
import com.nebula.app.domain.repositories.ProfileRepository
import com.nebula.app.domain.repositories.StreamingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecentGame(val session: GamingSession, val game: Game?)

data class HomeContent(
    val user: User?,
    val featured: List<Game>,
    val recent: List<RecentGame>,
    val upcoming: Booking?,
    val announcements: List<Announcement>,
    val favouriteIds: Set<String>,
    val unreadNotifications: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    auth: AuthRepository,
    private val games: GameRepository,
    bookings: BookingRepository,
    profile: ProfileRepository,
    streaming: StreamingRepository
) : ViewModel() {

    private val reload = MutableStateFlow(0)

    val sessionState: StateFlow<StreamingState> = streaming.client.state

    val state: StateFlow<Loadable<HomeContent>> = reload.flatMapLatest {
        val announcements = games.announcements()
        val base: Flow<HomeContent> = combine(
            auth.session,
            games.games,
            games.favouriteIds,
            games.recentSessions,
            bookings.bookings
        ) { user, allGames, favourites, sessions, allBookings ->
            HomeContent(
                user = user,
                featured = allGames.filter { it.featured }.ifEmpty { allGames.take(5) },
                recent = sessions.sortedByDescending { it.playedOn }
                    .map { s -> RecentGame(s, allGames.firstOrNull { it.id == s.gameId }) },
                upcoming = allBookings
                    .filter { it.status == BookingStatus.Upcoming }
                    .minByOrNull { it.start },
                announcements = announcements,
                favouriteIds = favourites,
                unreadNotifications = 0
            )
        }
        base.combine(profile.notifications) { content, notifications ->
            content.copy(unreadNotifications = notifications.count { !it.read })
        }
            .map<HomeContent, Loadable<HomeContent>> { Loadable.Ready(it) }
            .onStart { emit(Loadable.Loading) }
            .catch { emit(Loadable.Failed(it.userMessage())) }
    }.stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    fun retry() {
        reload.value = reload.value + 1
    }

    fun toggleFavourite(gameId: String) {
        viewModelScope.launch { games.toggleFavourite(gameId) }
    }
}
