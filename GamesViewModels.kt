package com.nebula.app.features.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.UiSharingStarted
import com.nebula.app.core.common.userMessage
import com.nebula.app.domain.GameCategories
import com.nebula.app.domain.filterGames
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.repositories.GameRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GamesContent(
    val query: String,
    val category: String,
    val categories: List<String>,
    val games: List<Game>,
    val totalGames: Int,
    val favouriteIds: Set<String>
)

@OptIn(ExperimentalCoroutinesApi::class)
class GamesViewModel(private val repository: GameRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow(GameCategories.ALL)
    private val reload = MutableStateFlow(0)

    val state: StateFlow<Loadable<GamesContent>> = reload.flatMapLatest {
        combine(repository.games, repository.favouriteIds, query, category) { games, favourites, q, c ->
            GamesContent(
                query = q,
                category = c,
                categories = GameCategories.forGames(games),
                games = filterGames(games, q, c, favourites),
                totalGames = games.size,
                favouriteIds = favourites
            )
        }
            .map<GamesContent, Loadable<GamesContent>> { Loadable.Ready(it) }
            .onStart { emit(Loadable.Loading) }
            .catch { emit(Loadable.Failed(it.userMessage())) }
    }.stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    fun onQueryChange(value: String) { query.value = value }
    fun onCategoryChange(value: String) { category.value = value }
    fun clearFilters() {
        query.value = ""
        category.value = GameCategories.ALL
    }
    fun retry() { reload.value = reload.value + 1 }
    fun toggleFavourite(gameId: String) {
        viewModelScope.launch { repository.toggleFavourite(gameId) }
    }
}

data class GameDetailContent(val game: Game, val isFavourite: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModel(
    private val repository: GameRepository,
    private val gameId: String
) : ViewModel() {

    private val reload = MutableStateFlow(0)

    val state: StateFlow<Loadable<GameDetailContent>> = reload.flatMapLatest {
        repository.favouriteIds
            .map<Set<String>, Loadable<GameDetailContent>> { favourites ->
                val game = repository.getGame(gameId)
                if (game == null) Loadable.Failed("This game could not be found.")
                else Loadable.Ready(GameDetailContent(game, game.id in favourites))
            }
            .onStart { emit(Loadable.Loading) }
            .catch { emit(Loadable.Failed(it.userMessage())) }
    }.stateIn(viewModelScope, UiSharingStarted, Loadable.Loading)

    fun retry() { reload.value = reload.value + 1 }
    fun toggleFavourite() {
        viewModelScope.launch { repository.toggleFavourite(gameId) }
    }
}
