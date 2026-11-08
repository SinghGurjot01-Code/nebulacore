package com.nebula.app.data.repositories

import com.nebula.app.data.local.DemoContent
import com.nebula.app.data.local.DemoGames
import com.nebula.app.data.local.KeyValueStore
import com.nebula.app.domain.models.Announcement
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GamingSession
import com.nebula.app.domain.repositories.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class LocalGameRepository(
    private val store: KeyValueStore,
    clock: Clock = Clock.systemDefaultZone()
) : GameRepository {

    private val catalogue = MutableStateFlow(DemoGames.all)

    override val games: Flow<List<Game>> = catalogue

    override val favouriteIds: Flow<Set<String>> =
        store.observe(KEY_FAVOURITES).map { raw ->
            raw.orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        }

    private val sessions: List<GamingSession> = run {
        val today = LocalDate.now(clock)
        fun session(id: String, gameId: String, daysAgo: Long, minutes: Int): GamingSession? {
            val game = DemoGames.all.firstOrNull { it.id == gameId } ?: return null
            return GamingSession(id, game.id, game.title, today.minusDays(daysAgo), minutes)
        }
        listOfNotNull(
            session("s1", "cyberpunk", 2, 95),
            session("s2", "god-of-war", 5, 120),
            session("s3", "elden", 9, 60),
            session("s4", "forza", 14, 45)
        )
    }

    override val recentSessions: Flow<List<GamingSession>> = MutableStateFlow(sessions)

    override suspend fun getGame(id: String): Game? = catalogue.value.firstOrNull { it.id == id }

    override suspend fun toggleFavourite(gameId: String) {
        val current = favouriteIds.first()
        val next = if (gameId in current) current - gameId else current + gameId
        store.put(KEY_FAVOURITES, next.joinToString(","))
    }

    override suspend fun announcements(): List<Announcement> = DemoContent.announcements

    private companion object {
        const val KEY_FAVOURITES = "favourite_game_ids"
    }
}
