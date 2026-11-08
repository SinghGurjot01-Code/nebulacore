package com.nebula.app.domain

import com.nebula.app.domain.models.Game

object GameCategories {
    const val ALL = "All"
    const val POPULAR = "Popular"
    const val FAVOURITES = "Favourites"

    fun forGames(games: List<Game>): List<String> =
        listOf(ALL, POPULAR, FAVOURITES) + games.map { it.genre }.distinct().sorted()
}

/** Pure filtering logic so it can be unit tested without Android. */
fun filterGames(
    games: List<Game>,
    query: String,
    category: String,
    favouriteIds: Set<String>
): List<Game> {
    val q = query.trim().lowercase()
    return games.filter { game ->
        val inCategory = when (category) {
            GameCategories.ALL -> true
            GameCategories.POPULAR -> game.popular
            GameCategories.FAVOURITES -> game.id in favouriteIds
            else -> game.genre.equals(category, ignoreCase = true)
        }
        val matchesQuery = q.isEmpty() ||
            game.title.lowercase().contains(q) ||
            game.studio.lowercase().contains(q) ||
            game.genre.lowercase().contains(q)
        inCategory && matchesQuery
    }
}
