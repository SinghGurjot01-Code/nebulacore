package com.nebula.app.data.remote

import com.nebula.app.domain.models.Announcement
import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GameAvailability
import com.nebula.app.domain.models.GamingSession
import com.nebula.app.domain.repositories.GameRepository
import com.nebula.app.domain.repositories.NebulaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

class RemoteGameRepository(private val client: SupabaseClient) : GameRepository {

    private val favourites = MutableStateFlow<Set<String>>(emptySet())

    override val games: Flow<List<Game>> = flow { emit(fetchGames()) }

    override val favouriteIds: Flow<Set<String>> = favourites.onStart { refreshFavourites() }

    override val recentSessions: Flow<List<GamingSession>> = flow {
        emit(
            try {
                fetchHistory()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    override suspend fun getGame(id: String): Game? {
        val response = client.request("GET", "/rest/v1/games?select=*&id=eq.${enc(id)}", authenticated = false)
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load this game."))
        val array = JSONArray(response.body)
        return if (array.length() == 0) null else toGame(array.getJSONObject(0))
    }

    override suspend fun toggleFavourite(gameId: String) {
        val previous = favourites.value
        val adding = gameId !in previous
        favourites.value = if (adding) previous + gameId else previous - gameId
        val success = try {
            val response = if (adding) {
                client.request(
                    "POST", "/rest/v1/favourites",
                    JSONObject().put("game_id", gameId).toString(),
                    mapOf("Prefer" to "return=minimal,resolution=ignore-duplicates")
                )
            } else {
                client.request("DELETE", "/rest/v1/favourites?game_id=eq.${enc(gameId)}")
            }
            response.ok
        } catch (e: NebulaException) {
            false
        }
        if (!success) favourites.value = previous
    }

    override suspend fun announcements(): List<Announcement> {
        return try {
            val response = client.request(
                "GET", "/rest/v1/announcements?select=id,title,body&order=created_at.desc",
                authenticated = false
            )
            if (!response.ok) return emptyList()
            val array = JSONArray(response.body)
            (0 until array.length()).map {
                val o = array.getJSONObject(it)
                Announcement(o.getString("id"), o.getString("title"), o.getString("body"))
            }
        } catch (e: NebulaException) {
            emptyList()
        }
    }

    private suspend fun fetchGames(): List<Game> {
        val response = client.request("GET", "/rest/v1/games?select=*&order=sort_order.asc", authenticated = false)
        if (!response.ok) throw NebulaException(response.errorMessage("Could not load games."))
        val array = JSONArray(response.body)
        return (0 until array.length()).map { toGame(array.getJSONObject(it)) }
    }

    private suspend fun refreshFavourites() {
        try {
            val response = client.request("GET", "/rest/v1/favourites?select=game_id")
            if (response.ok) {
                val array = JSONArray(response.body)
                favourites.value = (0 until array.length()).map { array.getJSONObject(it).getString("game_id") }.toSet()
            } else if (response.code == 401) {
                favourites.value = emptySet()
            }
        } catch (e: NebulaException) {
            // Keep the last known favourites when offline.
        }
    }

    private suspend fun fetchHistory(): List<GamingSession> {
        val now = enc(Instant.now().toString())
        val response = client.request(
            "GET",
            "/rest/v1/bookings?select=id,game_id,starts_at,duration_minutes,games(title)" +
                "&game_id=not.is.null&status=neq.cancelled&ends_at=lt.$now&order=starts_at.desc&limit=10"
        )
        if (!response.ok) return emptyList()
        val array = JSONArray(response.body)
        val zone = ZoneId.systemDefault()
        return (0 until array.length()).map {
            val o = array.getJSONObject(it)
            GamingSession(
                id = o.getString("id"),
                gameId = o.getString("game_id"),
                gameTitle = o.optJSONObject("games")?.str("title") ?: o.getString("game_id"),
                playedOn = OffsetDateTime.parse(o.getString("starts_at")).atZoneSameInstant(zone).toLocalDate(),
                durationMinutes = o.getInt("duration_minutes")
            )
        }
    }

    private fun toGame(o: JSONObject): Game {
        val appId = o.getInt("steam_app_id")
        val cdn = "https://cdn.akamai.steamstatic.com/steam/apps/$appId"
        return Game(
            id = o.getString("id"),
            title = o.getString("title"),
            studio = o.getString("studio"),
            genre = o.getString("genre"),
            description = o.getString("description"),
            coverUrl = "$cdn/library_600x900.jpg",
            heroUrl = "$cdn/library_hero.jpg",
            availability = when (o.optString("availability")) {
                "high_demand" -> GameAvailability.HighDemand
                "coming_soon" -> GameAvailability.ComingSoon
                else -> GameAvailability.Available
            },
            popular = o.optBoolean("popular"),
            featured = o.optBoolean("featured")
        )
    }
}
