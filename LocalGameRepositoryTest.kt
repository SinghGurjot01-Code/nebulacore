package com.nebula.app.data

import com.nebula.app.data.local.InMemoryKeyValueStore
import com.nebula.app.data.repositories.LocalGameRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalGameRepositoryTest {

    private fun repository() = LocalGameRepository(InMemoryKeyValueStore())

    @Test
    fun catalogueIsNotEmptyAndIdsAreUnique() = runTest {
        val games = repository().games.first()
        assertTrue(games.isNotEmpty())
        assertEquals(games.size, games.map { it.id }.toSet().size)
    }

    @Test
    fun favouriteTogglesOnAndOff() = runTest {
        val repo = repository()
        val id = repo.games.first().first().id
        assertTrue(repo.favouriteIds.first().isEmpty())
        repo.toggleFavourite(id)
        assertEquals(setOf(id), repo.favouriteIds.first())
        repo.toggleFavourite(id)
        assertTrue(repo.favouriteIds.first().isEmpty())
    }

    @Test
    fun getGameFindsKnownIdsOnly() = runTest {
        val repo = repository()
        assertNotNull(repo.getGame("cyberpunk"))
        assertNull(repo.getGame("does-not-exist"))
    }

    @Test
    fun unreleasedGamesCannotBePlayed() = runTest {
        val wolf = repository().getGame("wolf2")
        assertEquals(false, wolf?.canPlay)
    }

    @Test
    fun hasAnnouncementsAndHistory() = runTest {
        val repo = repository()
        assertTrue(repo.announcements().isNotEmpty())
        assertTrue(repo.recentSessions.first().isNotEmpty())
    }
}
