package com.nebula.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.domain.Validation
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GamingSession
import com.nebula.app.domain.models.Notification
import com.nebula.app.domain.models.NotificationPreferences
import com.nebula.app.domain.models.User
import com.nebula.app.domain.repositories.AuthRepository
import com.nebula.app.domain.repositories.GameRepository
import com.nebula.app.domain.repositories.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val auth: AuthRepository,
    games: GameRepository
) : ViewModel() {
    val user: StateFlow<User?> = auth.session.stateIn(viewModelScope, UiSharingStarted, null)
    val favouriteCount: StateFlow<Int> = games.favouriteIds.map { it.size }.stateIn(viewModelScope, UiSharingStarted, 0)
    val sessionCount: StateFlow<Int> = games.recentSessions.map { it.size }.stateIn(viewModelScope, UiSharingStarted, 0)

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.logout()
            onDone()
        }
    }
}

data class EditProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val nameError: String? = null,
    val formError: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false
)

class EditProfileViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = auth.session.first()
            _state.update { it.copy(fullName = user?.fullName.orEmpty(), email = user?.email.orEmpty(), loading = false) }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(fullName = value, nameError = null, formError = null) }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        val error = Validation.fullName(s.fullName)
        if (error != null) {
            _state.update { it.copy(nameError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, formError = null) }
            auth.updateFullName(s.fullName)
                .onSuccess {
                    _state.update { it.copy(saving = false) }
                    onSaved()
                }
                .onFailure { e -> _state.update { it.copy(saving = false, formError = e.userMessage()) } }
        }
    }
}

class FavouritesViewModel(private val repository: GameRepository) : ViewModel() {
    val state: StateFlow<Loadable<List<Game>>> = combine(repository.games, repository.favouriteIds) { games, ids ->
        games.filter { it.id in ids }
    }
        .map<List<Game>, Loadable<List<Game>>> { Loadable.Ready(it) }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    fun toggle(gameId: String) {
        viewModelScope.launch { repository.toggleFavourite(gameId) }
    }
}

class HistoryViewModel(repository: GameRepository) : ViewModel() {
    val state: StateFlow<Loadable<List<GamingSession>>> = repository.recentSessions
        .map<List<GamingSession>, Loadable<List<GamingSession>>> { list ->
            Loadable.Ready(list.sortedByDescending { it.playedOn })
        }
        .onStart { emit(Loadable.Loading) }
        .catch { emit(Loadable.Failed(it.userMessage())) }
        .stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)
}

class NotificationsViewModel(private val repository: ProfileRepository) : ViewModel() {
    val preferences: StateFlow<NotificationPreferences> = repository.notificationPreferences
        .stateIn(viewModelScope, UiSharingStarted, NotificationPreferences())
    val notifications: StateFlow<List<Notification>> = repository.notifications
        .stateIn(viewModelScope, UiSharingStarted, emptyList())

    fun setBookingReminders(enabled: Boolean) { viewModelScope.launch { repository.setBookingReminders(enabled) } }
    fun setSupportUpdates(enabled: Boolean) { viewModelScope.launch { repository.setSupportUpdates(enabled) } }
    fun setAnnouncements(enabled: Boolean) { viewModelScope.launch { repository.setAnnouncements(enabled) } }
    fun markAllRead() { viewModelScope.launch { repository.markAllNotificationsRead() } }
}
