package com.nebula.app.features.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.EmptyState
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.GameImage
import com.nebula.app.design.components.GameTile
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaTextField
import com.nebula.app.design.components.ShimmerBox
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.components.availabilityTone
import com.nebula.app.design.theme.NebulaColors
import com.nebula.app.domain.GameCategories

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) NebulaColors.Ink else NebulaColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else NebulaColors.Ink,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun GamesScreen(innerPadding: PaddingValues, onOpenGame: (String) -> Unit) {
    val viewModel = nebulaViewModel { GamesViewModel(it.gameRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Games", style = MaterialTheme.typography.headlineMedium)
            NebulaTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.onQueryChange(it)
                },
                label = "Search games",
                leadingIcon = Icons.Filled.Search,
                imeAction = ImeAction.Search
            )
        }

        when (val current = state) {
            is Loadable.Loading -> GamesLoading(innerPadding)
            is Loadable.Failed -> ErrorState(current.message, onRetry = viewModel::retry)
            is Loadable.Ready -> {
                val content = current.value
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(content.categories.size) { index ->
                        val category = content.categories[index]
                        CategoryChip(category, category == content.category) { viewModel.onCategoryChange(category) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (content.games.isEmpty()) {
                    val isFavourites = content.category == GameCategories.FAVOURITES && content.query.isBlank()
                    EmptyState(
                        icon = if (isFavourites) Icons.Filled.Favorite else Icons.Filled.Search,
                        title = if (isFavourites) "No favourites yet" else "No games found",
                        message = if (isFavourites) "Tap the heart on a game to save it here."
                        else "Try a different search or category.",
                        actionLabel = "Clear filters",
                        onAction = {
                            searchText = ""
                            viewModel.clearFilters()
                        }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "${content.games.size} of ${content.totalGames} games",
                                style = MaterialTheme.typography.bodySmall,
                                color = NebulaColors.Secondary
                            )
                        }
                        items(content.games, key = { it.id }) { game ->
                            GameTile(
                                game = game,
                                isFavourite = game.id in content.favouriteIds,
                                onClick = { onOpenGame(game.id) },
                                onToggleFavourite = { viewModel.toggleFavourite(game.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesLoading(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = innerPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ShimmerBox(Modifier.weight(1f).aspectRatio(3f / 4f))
                ShimmerBox(Modifier.weight(1f).aspectRatio(3f / 4f))
            }
        }
    }
}

@Composable
fun GameDetailScreen(
    gameId: String,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onBook: () -> Unit
) {
    val viewModel = nebulaViewModel(key = "game-$gameId") { GameDetailViewModel(it.gameRepository, gameId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        is Loadable.Loading -> Box(Modifier.fillMaxSize()) {
            ShimmerBox(Modifier.fillMaxWidth().aspectRatio(16f / 10f), RoundedCornerShape(0.dp))
        }
        is Loadable.Failed -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            ErrorState(current.message, onRetry = viewModel::retry)
        }
        is Loadable.Ready -> {
            val game = current.value.game
            val favourite = current.value.isFavourite
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                    GameImage(game.heroUrl, game.title, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.35f),
                                    0.4f to Color.Transparent,
                                    1f to NebulaColors.Background
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        IconButton(
                            onClick = viewModel::toggleFavourite,
                            modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (favourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (favourite) "Remove from favourites" else "Add to favourites",
                                tint = if (favourite) NebulaColors.Accent else NebulaColors.Ink
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(game.title, style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(game.genre, ChipTone.Neutral)
                        StatusChip(game.availability.label, availabilityTone(game.availability))
                    }
                    Text(game.studio, style = MaterialTheme.typography.titleSmall, color = NebulaColors.Secondary)
                    Text(game.description, style = MaterialTheme.typography.bodyLarge)

                    InfoBanner(
                        text = "Cloud streaming is not connected yet. Play now runs the pre-session checks and explains what is still missing.",
                        icon = Icons.Filled.Info
                    )
                    NebulaButton(
                        text = if (game.canPlay) "Play now" else "Not released yet",
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth(),
                        kind = ButtonKind.Accent,
                        enabled = game.canPlay,
                        icon = Icons.Filled.PlayArrow
                    )
                    NebulaButton(
                        text = "Book a session",
                        onClick = onBook,
                        modifier = Modifier.fillMaxWidth(),
                        kind = ButtonKind.Primary,
                        enabled = game.canPlay
                    )
                    if (!game.canPlay) {
                        Text(
                            "This title is listed for preview only, so playing and booking are disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NebulaColors.Secondary
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
