package com.nebula.app.domain

import com.nebula.app.data.local.DemoGames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameFilterTest {

    private val games = DemoGames.all

    @Test
    fun allCategoryReturnsEverything() {
        assertEquals(games.size, filterGames(games, "", GameCategories.ALL, emptySet()).size)
    }

    @Test
    fun searchMatchesTitleStudioAndGenre() {
        val byTitle = filterGames(games, "cyberpunk", GameCategories.ALL, emptySet())
        assertTrue(byTitle.isNotEmpty() && byTitle.all { it.title.contains("Cyberpunk", ignoreCase = true) })

        val byStudio = filterGames(games, "capcom", GameCategories.ALL, emptySet())
        assertTrue(byStudio.isNotEmpty() && byStudio.all { it.studio.contains("Capcom", ignoreCase = true) })
    }

    @Test
    fun genreCategoryFilters() {
        val horror = filterGames(games, "", "Horror", emptySet())
        assertTrue(horror.isNotEmpty() && horror.all { it.genre == "Horror" })
    }

    @Test
    fun popularCategoryOnlyReturnsPopularGames() {
        val popular = filterGames(games, "", GameCategories.POPULAR, emptySet())
        assertTrue(popular.isNotEmpty() && popular.all { it.popular })
    }

    @Test
    fun favouritesCategoryUsesFavouriteIds() {
        val ids = setOf(games[0].id, games[3].id)
        val result = filterGames(games, "", GameCategories.FAVOURITES, ids)
        assertEquals(ids, result.map { it.id }.toSet())
        assertTrue(filterGames(games, "", GameCategories.FAVOURITES, emptySet()).isEmpty())
    }

    @Test
    fun queryAndCategoryCombine() {
        val result = filterGames(games, "evil", "Horror", emptySet())
        assertTrue(result.isNotEmpty() && result.all { it.genre == "Horror" })
        assertTrue(filterGames(games, "zzzz-no-match", GameCategories.ALL, emptySet()).isEmpty())
    }

    @Test
    fun categoriesIncludeFixedEntriesAndGenres() {
        val categories = GameCategories.forGames(games)
        assertEquals(listOf("All", "Popular", "Favourites"), categories.take(3))
        assertTrue("Action" in categories)
    }
}
