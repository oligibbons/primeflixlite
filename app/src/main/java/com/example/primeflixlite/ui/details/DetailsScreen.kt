package com.example.primeflixlite.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.data.parser.xtream.XtreamChannelInfo
import com.example.primeflixlite.ui.theme.*

@Composable
fun DetailsScreen(
    channel: Channel,
    viewModel: DetailsViewModel,
    imageLoader: coil.ImageLoader,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val meta = uiState.metadata
    val displayChannel = viewModel.currentChannel.collectAsState().value ?: channel
    val context = LocalContext.current

    LaunchedEffect(channel) {
        viewModel.loadContent(channel)
    }

    BackHandler { onBack() }

    // --- 1. BACKGROUND LAYER ---
    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        val backdropUrl = meta?.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
            ?: displayChannel.cover

        if (!backdropUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .size(1280, 720)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f) // Darkened for readability
            )
            // Gradient Overlay (Top to Bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, VoidBlack),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            // LEFT: POSTER
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.width(300.dp).aspectRatio(2f/3f)
            ) {
                val posterUrl = meta?.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    ?: displayChannel.cover

                if (!posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(posterUrl).size(400, 600).build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(OffBlack))
                }
            }

            Spacer(Modifier.width(48.dp))

            // RIGHT: INFO & CONTROLS
            Column(modifier = Modifier.weight(1f)) {

                // Title
                Text(
                    text = meta?.title ?: displayChannel.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Meta Row (Year | Genre | Rating)
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (meta?.voteAverage != null && meta.voteAverage > 0) {
                        Badge(text = "★ ${String.format("%.1f", meta.voteAverage)}", color = NeonYellow)
                    }
                    if (!meta?.genres.isNullOrEmpty()) {
                        Text(text = meta!!.genres, color = LightGray, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Description
                Text(
                    text = meta?.overview ?: "No description available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LightGray,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // ACTIONS ROW
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Primary Play Button
                    val playUrl = if (uiState.versions.isNotEmpty()) uiState.versions.first().url else displayChannel.url
                    NeonButton(
                        text = "PLAY MOVIE",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = { onPlayClick(playUrl) }
                    )

                    // Series Season Selector (if applicable)
                    if (displayChannel.type == StreamType.SERIES.name && uiState.episodes.isNotEmpty()) {
                        val seasons = viewModel.getSeasons()
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { seasonNum ->
                                FilterChip(
                                    selected = viewModel.selectedSeason == seasonNum,
                                    onClick = { viewModel.selectedSeason = seasonNum },
                                    label = { Text("Season $seasonNum") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonBlue,
                                        labelColor = White
                                    )
                                )
                            }
                        }
                    }
                }

                // EPISODE LIST (For Series)
                if (displayChannel.type == StreamType.SERIES.name) {
                    Spacer(Modifier.height(24.dp))
                    Text("EPISODES", color = NeonBlue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(viewModel.getEpisodesForSeason(viewModel.selectedSeason)) { episode ->
                            EpisodeRow(
                                episode = episode,
                                onClick = {
                                    // Construct URL logic here same as before
                                    val ext = episode.containerExtension ?: "mkv"
                                    val url = "${displayChannel.url.substringBefore("/series/")}/series/${displayChannel.url.split("/")[4]}/${displayChannel.url.split("/")[5]}/${episode.id}.$ext"
                                    onPlayClick(url)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) White else if (isPrimary) NeonYellow else OffBlack,
            contentColor = if (isFocused) VoidBlack else if (isPrimary) VoidBlack else White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1f)
            .border(
                width = if (!isPrimary && !isFocused) 2.dp else 0.dp,
                color = if (!isPrimary && !isFocused) Color.Gray else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EpisodeRow(episode: XtreamChannelInfo.Episode, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) White else OffBlack)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${episode.episodeNum}",
            color = if (isFocused) VoidBlack else NeonBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = episode.title ?: "Episode ${episode.episodeNum}",
            color = if (isFocused) VoidBlack else White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}