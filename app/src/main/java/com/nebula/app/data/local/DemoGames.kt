package com.nebula.app.data.local

import com.nebula.app.domain.models.Game
import com.nebula.app.domain.models.GameAvailability

/**
 * Demo catalogue used by the local repositories. Titles and Steam app ids are
 * only used to show public store artwork; nothing here implies the games are
 * playable through Nebula yet.
 */
object DemoGames {

    private const val CDN = "https://cdn.akamai.steamstatic.com/steam/apps"

    private fun game(
        id: String,
        title: String,
        studio: String,
        genre: String,
        steamAppId: Int,
        availability: GameAvailability,
        popular: Boolean,
        featured: Boolean
    ) = Game(
        id = id,
        title = title,
        studio = studio,
        genre = genre,
        description = "$title by $studio. A $genre title in the Nebula demo catalogue. " +
            "Cloud streaming is not connected yet, so this is a preview of how the game will appear.",
        coverUrl = "$CDN/$steamAppId/library_600x900.jpg",
        heroUrl = "$CDN/$steamAppId/library_hero.jpg",
        availability = availability,
        popular = popular,
        featured = featured
    )

    val all: List<Game> = listOf(
        game("god-of-war", "God of War", "Santa Monica Studio", "Action", 1593500, GameAvailability.Available, popular = true, featured = true),
        game("gta-v", "GTA V", "Rockstar Games", "Action", 271590, GameAvailability.Available, popular = true, featured = true),
        game("rdr2", "Red Dead Redemption 2", "Rockstar Games", "Adventure", 1174180, GameAvailability.Available, popular = true, featured = true),
        game("cyberpunk", "Cyberpunk 2077", "CD Projekt Red", "RPG", 1091500, GameAvailability.Available, popular = true, featured = true),
        game("forza", "Forza Horizon 5", "Playground Games", "Racing", 1551360, GameAvailability.HighDemand, popular = true, featured = false),
        game("witcher3", "The Witcher 3", "CD Projekt Red", "RPG", 292030, GameAvailability.Available, popular = true, featured = true),
        game("civ6", "Civilization VI", "Firaxis", "Strategy", 289070, GameAvailability.Available, popular = false, featured = false),
        game("elden", "Elden Ring", "FromSoftware", "Action", 1245620, GameAvailability.Available, popular = true, featured = true),
        game("spiderman-remastered", "Marvel's Spider-Man Remastered", "Insomniac Games", "Action", 1817070, GameAvailability.Available, popular = true, featured = false),
        game("spiderman-miles", "Marvel's Spider-Man: Miles Morales", "Insomniac Games", "Action", 1817190, GameAvailability.Available, popular = false, featured = false),
        game("rdr1", "Red Dead Redemption", "Rockstar Games", "Adventure", 2668510, GameAvailability.Available, popular = false, featured = false),
        game("gow-ragnarok", "God of War Ragnarök", "Santa Monica Studio", "Action", 2322010, GameAvailability.Available, popular = true, featured = false),
        game("re2", "Resident Evil 2", "Capcom", "Horror", 883710, GameAvailability.Available, popular = false, featured = false),
        game("re3", "Resident Evil 3", "Capcom", "Horror", 952060, GameAvailability.HighDemand, popular = false, featured = false),
        game("re4", "Resident Evil 4", "Capcom", "Horror", 2050650, GameAvailability.Available, popular = true, featured = false),
        game("re7", "Resident Evil 7 Biohazard", "Capcom", "Horror", 418370, GameAvailability.Available, popular = false, featured = false),
        game("re-village", "Resident Evil Village", "Capcom", "Horror", 1196590, GameAvailability.Available, popular = false, featured = false),
        game("re9", "Resident Evil Requiem", "Capcom", "Horror", 3764200, GameAvailability.Available, popular = false, featured = false),
        game("twd-s1", "The Walking Dead: Season 1", "Telltale Games", "Adventure", 207610, GameAvailability.Available, popular = false, featured = false),
        game("twd-s2", "The Walking Dead: Season 2", "Telltale Games", "Adventure", 261030, GameAvailability.Available, popular = false, featured = false),
        game("twd-nf", "The Walking Dead: A New Frontier", "Telltale Games", "Adventure", 536220, GameAvailability.Available, popular = false, featured = false),
        game("twd-final", "The Walking Dead: The Final Season", "Telltale Games", "Adventure", 866970, GameAvailability.Available, popular = false, featured = false),
        game("tlou1", "The Last of Us Part I", "Naughty Dog", "Adventure", 1888930, GameAvailability.HighDemand, popular = true, featured = false),
        game("tlou2", "The Last of Us Part II Remastered", "Naughty Dog", "Adventure", 2531310, GameAvailability.Available, popular = false, featured = false),
        game("sh-f", "Silent Hill f", "Konami", "Horror", 2947440, GameAvailability.Available, popular = false, featured = false),
        game("sh2", "Silent Hill 2", "Konami", "Horror", 2124490, GameAvailability.Available, popular = true, featured = false),
        game("plague-tale", "A Plague Tale: Innocence", "Asobo Studio", "Adventure", 752590, GameAvailability.Available, popular = false, featured = false),
        game("detroit", "Detroit: Become Human", "Quantic Dream", "Adventure", 1222140, GameAvailability.Available, popular = false, featured = false),
        game("wolf1", "The Wolf Among Us", "Telltale Games", "Adventure", 250320, GameAvailability.Available, popular = false, featured = false),
        game("wolf2", "The Wolf Among Us 2", "Telltale Games", "Adventure", 1884870, GameAvailability.ComingSoon, popular = false, featured = false),
        game("lis1", "Life Is Strange", "Dontnod", "Adventure", 319630, GameAvailability.Available, popular = false, featured = false),
        game("lis2", "Life Is Strange 2", "Dontnod", "Adventure", 532210, GameAvailability.HighDemand, popular = false, featured = false),
        game("lis-bts", "Life Is Strange: Before the Storm", "Deck Nine", "Adventure", 554620, GameAvailability.Available, popular = false, featured = false),
        game("swtd", "Still Wakes the Deep", "The Chinese Room", "Horror", 1622100, GameAvailability.Available, popular = false, featured = false),
        game("dpa-ashes", "The Dark Pictures Anthology: House of Ashes", "Supermassive Games", "Horror", 1281590, GameAvailability.Available, popular = false, featured = false),
        game("hellblade", "Hellblade: Senua's Sacrifice", "Ninja Theory", "Action", 414340, GameAvailability.Available, popular = false, featured = false),
        game("hellblade2", "Senua's Saga: Hellblade II", "Ninja Theory", "Action", 2461850, GameAvailability.Available, popular = true, featured = false),
        game("evil1", "The Evil Within", "Tango Gameworks", "Horror", 268050, GameAvailability.Available, popular = false, featured = false),
        game("evil2", "The Evil Within 2", "Tango Gameworks", "Horror", 601430, GameAvailability.Available, popular = false, featured = false),
        game("sekiro", "Sekiro: Shadows Die Twice", "FromSoftware", "Action", 814380, GameAvailability.Available, popular = true, featured = false),
        game("subnautica", "Subnautica", "Unknown Worlds", "Adventure", 264710, GameAvailability.HighDemand, popular = false, featured = false),
        game("uncharted-ll", "Uncharted: The Lost Legacy", "Naughty Dog", "Adventure", 1659420, GameAvailability.Available, popular = false, featured = false),
        game("dying-light", "Dying Light", "Techland", "Action", 239140, GameAvailability.Available, popular = false, featured = false),
        game("tomb-raider", "Tomb Raider", "Crystal Dynamics", "Adventure", 203160, GameAvailability.Available, popular = false, featured = false),
        game("rise-tr", "Rise of the Tomb Raider", "Crystal Dynamics", "Adventure", 391220, GameAvailability.Available, popular = false, featured = false),
        game("shadow-tr", "Shadow of the Tomb Raider", "Eidos-Montréal", "Adventure", 750920, GameAvailability.Available, popular = false, featured = false),
        game("tr1-3", "Tomb Raider I-III Remastered", "Aspyr", "Adventure", 2478970, GameAvailability.Available, popular = false, featured = false),
        game("tr4-6", "Tomb Raider IV-VI Remastered", "Aspyr", "Adventure", 2525380, GameAvailability.Available, popular = false, featured = false),
        game("tr-legend", "Tomb Raider: Legend", "Crystal Dynamics", "Adventure", 7000, GameAvailability.Available, popular = false, featured = false),
        game("tr-anniversary", "Tomb Raider: Anniversary", "Crystal Dynamics", "Adventure", 8000, GameAvailability.HighDemand, popular = false, featured = false),
        game("tr-underworld", "Tomb Raider: Underworld", "Crystal Dynamics", "Adventure", 8140, GameAvailability.Available, popular = false, featured = false),
        game("tr-gol", "Lara Croft and the Guardian of Light", "Crystal Dynamics", "Adventure", 21000, GameAvailability.Available, popular = false, featured = false),
        game("tr-ooo", "Lara Croft and the Temple of Osiris", "Crystal Dynamics", "Adventure", 289690, GameAvailability.Available, popular = false, featured = false),
        game("bioshock", "BioShock Remastered", "2K", "Action", 409710, GameAvailability.Available, popular = true, featured = false),
        game("expedition33", "Clair Obscur: Expedition 33", "Sandfall Interactive", "RPG", 1903340, GameAvailability.Available, popular = true, featured = false),
    )
}
