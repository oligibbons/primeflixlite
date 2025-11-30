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
import androidx.compose.ui.unit.dp
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
    channel: Channel, // Placeholder passed from Nav
    viewModel: DetailsViewModel,
    imageLoader: coil.ImageLoader,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    // FIX: Observe the LIVE data from ViewModel
    val liveChannel by viewModel.currentChannel.collectAsState()

    // Use live data if available, otherwise fallback to placeholder
    val displayChannel = liveChannel ?: channel

    // Trigger load if we only have the placeholder
    LaunchedEffect(channel) {
        viewModel.loadContent(channel)
    }

    BackHandler { onBack() }

    // PERFORMANCE: Hoist gradients
    val backdropGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, VoidBlack),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }

    // PERFORMANCE: Resize backdrop
    val backdropRequest = remember(displayChannel.cover) {
        ImageRequest.Builder(LocalContext.current)
            .data(displayChannel.cover)
            .size(1280, 720)
            .crossfade(true)
            .build()
    }

    val posterRequest = remember(displayChannel.cover) {
        ImageRequest.Builder(LocalContext.current)
            .data(displayChannel.cover)
            .size(400, 600)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // --- BACKDROP ---
        if (!displayChannel.cover.isNullOrEmpty()) {
            AsyncImage(
                model = backdropRequest,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = backdropGradient)
                    }
            )
        }

        // --- CONTENT ---
        Row(modifier = Modifier.fillMaxSize().padding(40.dp)) {
            // LEFT: Poster
            Column(modifier = Modifier.width(300.dp)) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.aspectRatio(2f/3f)
                ) {
                    if (!displayChannel.cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = posterRequest,
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                            // Show loading text only if truly loading
                            if (displayChannel.title == "Loading...") {
                                CircularProgressIndicator(color = NeonBlue)
                            } else {
                                Text(displayChannel.title.take(1), style = MaterialTheme.typography.displayMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // RIGHT: Metadata & Episodes
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = displayChannel.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (displayChannel.type == StreamType.MOVIE.name) {
                    PlayButton(
                        onClick = { onPlayClick(displayChannel.url) },
                        label = "PLAY MOVIE"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Group: ${displayChannel.group}", color = Color.Gray)
                }
                else if (displayChannel.type == StreamType.SERIES.name) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = NeonBlue)
                    } else if (uiState.episodes.isNotEmpty()) {
                        // Season Selector
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Episode List
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
fun PlayButton(onClick: () -> Unit, label: String) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = BurntYellow),
        modifier = Modifier
            .height(50.dp)
            .focusRequester(focusRequester)
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