package com.example.primeflixlite.ui.details

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.primeflixlite.ui.theme.BurntYellow
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun DetailsScreen(
    channel: Channel,
    viewModel: DetailsViewModel,
    imageLoader: coil.ImageLoader,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val liveChannel by viewModel.currentChannel.collectAsState()
    val displayChannel = liveChannel ?: channel
    val context = LocalContext.current
    val meta = uiState.metadata

    LaunchedEffect(channel) {
        viewModel.loadContent(channel)
    }

    BackHandler { onBack() }

    val backdropGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, VoidBlack),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }

    // Prefer TMDB Backdrop, fallback to IPTV cover
    val backdropUrl = meta?.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
        ?: displayChannel.cover

    val backdropRequest = remember(backdropUrl) {
        ImageRequest.Builder(context)
            .data(backdropUrl)
            .size(1280, 720)
            .crossfade(true)
            .build()
    }

    // Prefer TMDB Poster, fallback to IPTV cover
    val posterUrl = meta?.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        ?: displayChannel.cover

    val posterRequest = remember(posterUrl) {
        ImageRequest.Builder(context)
            .data(posterUrl)
            .size(400, 600)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // --- BACKGROUND ---
        if (!backdropUrl.isNullOrEmpty()) {
            AsyncImage(
                model = backdropRequest,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp) // Taller backdrop
                    .alpha(0.5f)    // Dimmed
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = backdropGradient)
                    }
            )
        }

        // --- CONTENT ---
        Row(modifier = Modifier.fillMaxSize().padding(40.dp)) {

            // LEFT COLUMN: POSTER
            Column(modifier = Modifier.width(280.dp)) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.aspectRatio(2f/3f)
                ) {
                    if (!posterUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = posterRequest,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.DarkGray))
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // RIGHT COLUMN: INFO & ACTIONS
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. TITLE
                Text(
                    text = meta?.title ?: displayChannel.canonicalTitle ?: displayChannel.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. BADGES (Rating, Genres, Year)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (meta?.voteAverage != null && meta.voteAverage > 0.0) {
                        Badge(text = "★ ${String.format("%.1f", meta.voteAverage)}", color = BurntYellow)
                    }
                    meta?.genres?.let {
                        if (it.isNotEmpty()) Text(text = it, color = Color.LightGray, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. ACTIONS (Versions or Series Logic)
                if (displayChannel.type == StreamType.MOVIE.name) {

                    // MOVIE: Show Versions (4K, 1080p)
                    Text("Select Version:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.versions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.versions) { version ->
                                PlayButton(
                                    onClick = { onPlayClick(version.url) },
                                    label = if (version.quality.isNotEmpty()) version.quality else "PLAY",
                                    isHighlight = version.quality.contains("4K", true)
                                )
                            }
                        }
                    } else {
                        // Fallback if versions list empty (rare)
                        PlayButton(
                            onClick = { onPlayClick(displayChannel.url) },
                            label = "PLAY MOVIE"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                } else if (displayChannel.type == StreamType.SERIES.name) {

                    // SERIES: Episodes Logic
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = NeonBlue)
                    } else if (uiState.episodes.isNotEmpty()) {
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

                        // We put the episode list in a Box with weight so it scrolls properly
                        // inside this Column, separate from the Plot/Cast below
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 4. PLOT & CAST (Scrollable container for text)
                Column(modifier = Modifier.weight(1f).focusable(false)) {
                    if (meta?.overview != null) {
                        Text(
                            text = meta.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDDDDDD),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (meta?.castPreview != null) {
                        Text(
                            text = "Cast: ${meta.castPreview}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    // If Series, list episodes here (taking remaining space)
                    if (displayChannel.type == StreamType.SERIES.name && uiState.episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(
                                items = viewModel.getEpisodesForSeason(viewModel.selectedSeason),
                                key = { it.id ?: it.hashCode() }
                            ) { episode ->
                                EpisodeRow(
                                    episode = episode,
                                    onClick = {
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
}

@Composable
fun PlayButton(onClick: () -> Unit, label: String, isHighlight: Boolean = false) {
    var isFocused by remember { mutableStateOf(false) }

    // Only request focus initially if it's the first button (simplification)
    // In a real grid, you'd manage focusRequester refs more carefully.

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHighlight) NeonBlue else BurntYellow
        ),
        modifier = Modifier
            .height(50.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                if(isFocused) 3.dp else 0.dp,
                if(isFocused) White else Color.Transparent,
                RoundedCornerShape(25.dp)
            )
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VoidBlack)
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = VoidBlack, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EpisodeRow(episode: XtreamChannelInfo.Episode, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = if (isFocused) Color(0xFF333333) else Color(0xFF1A1A1A)
    val borderColor = if (isFocused) NeonBlue else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${episode.episodeNum}",
            color = NeonBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Text(text = episode.title ?: "Episode ${episode.episodeNum}", color = White)
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}