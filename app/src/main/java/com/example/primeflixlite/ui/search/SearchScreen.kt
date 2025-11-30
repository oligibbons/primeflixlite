package com.example.primeflixlite.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.ui.components.NeonTextField
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    imageLoader: coil.ImageLoader,
    onChannelClick: (Channel) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus the search bar on entry
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp)
    ) {
        // --- SEARCH HEADER ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f).focusRequester(focusRequester)) {
                NeonTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    label = "Search Channels, Movies, Series...",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }
        }

        // --- RESULTS AREA ---
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else if (uiState.results.isEmpty() && uiState.hasSearched) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No results in the Void", color = Color.Gray, fontSize = 18.sp)
                    Text("Try a different term", color = Color.DarkGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(uiState.results, key = { it.url }) { channel ->
                    SearchResultCard(
                        channel = channel,
                        imageLoader = imageLoader,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

// Hoist shape
private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun SearchResultCard(channel: Channel, imageLoader: coil.ImageLoader, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NeonBlue else Color.Transparent
    val context = LocalContext.current

    // OPTIMIZATION: Memoize image request
    val imageRequest = remember(channel.cover) {
        ImageRequest.Builder(context)
            .data(channel.cover)
            .size(220, 330) // ~140dp width
            .crossfade(false)
            .build()
    }

    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(CardShape)
            .border(2.dp, borderColor, CardShape)
            .background(Color(0xFF1E1E1E))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        // Poster / Icon
        Box(modifier = Modifier.aspectRatio(2f/3f).background(Color.Black)) {
            if (!channel.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = channel.title.take(1),
                        color = Color.Gray,
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }
            // Type Badge
            val badgeColor = when(channel.type) {
                StreamType.LIVE.name -> Color.Red
                StreamType.MOVIE.name -> NeonBlue
                StreamType.SERIES.name -> Color.Yellow
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(badgeColor.copy(alpha = 0.8f), RoundedCornerShape(bottomStart = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = channel.type.take(1),
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Title
        Text(
            text = channel.title,
            color = if (isFocused) White else Color.LightGray,
            fontSize = 12.sp,
            maxLines = 2,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}