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
    onPlayClick: (String) -> Unit, // URL to play
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initial Load
    LaunchedEffect(channel) {
        viewModel.loadContent(channel)
    }

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // --- BACKDROP ---
        if (!channel.cover.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(channel.cover).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .drawWithContent {
                        drawContent()
                        // Gradient Fade to Black
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, VoidBlack),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }
            )
        }

        // --- CONTENT ---
        Row(modifier = Modifier.fillMaxSize().padding(40.dp)) {
            // LEFT: Poster & Info
            Column(modifier = Modifier.width(300.dp)) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.aspectRatio(2f/3f)
                ) {
                    if (!channel.cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(channel.cover).build(),
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

            // RIGHT: Metadata & Episodes
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Play Button (If Movie) or Episodes List (If Series)
                if (channel.type == StreamType.MOVIE) {
                    PlayButton(
                        onClick = { onPlayClick(channel.url) },
                        label = "PLAY MOVIE"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Group: ${channel.group}", color = Color.Gray)
                }
                else if (channel.type == StreamType.SERIES) {
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
                            items(viewModel.getEpisodesForSeason(viewModel.selectedSeason)) { episode ->
                                EpisodeRow(
                                    episode = episode,
                                    // Use container_extension to build URL if not provided directly
                                    onClick = {
                                        // Construct Stream URL: basicUrl/series/user/pass/id.ext
                                        // Note: Logic usually handled in repo, simplified here
                                        val ext = episode.container_extension ?: "mkv"
                                        val url = "${channel.url.substringBefore("/series/")}/series/${channel.url.split("/")[4]}/${channel.url.split("/")[5]}/${episode.id}.$ext"
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
            text = "${episode.episode_num}",
            color = NeonBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        Text(text = episode.title ?: "Episode ${episode.episode_num}", color = White)
    }
}