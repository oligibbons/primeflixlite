package com.example.primeflixlite.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.primeflixlite.data.local.entity.Channel
import com.example.primeflixlite.data.local.entity.StreamType
import com.example.primeflixlite.ui.home.MovieCard
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    imageLoader: coil.ImageLoader,
    onChannelClick: (Channel) -> Unit,
    onBack: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(24.dp)
    ) {
        // --- HEADER & SEARCH BAR ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            // Back Button
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onBack() }
                    .focusable()
            )

            Spacer(modifier = Modifier.width(24.dp))

            // Search Input Field
            var isFocused by remember { mutableStateOf(false) }
            val borderColor = if (isFocused) NeonBlue else Color.DarkGray

            BasicTextField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
                cursorBrush = SolidColor(NeonBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { defaultKeyboardAction(ImeAction.Search) }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        if (query.isEmpty()) {
                            Text("Search Channels, Movies, Series...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
            )
        }

        // --- RESULTS ---
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else if (results.isEmpty() && query.length >= 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found for \"$query\"", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(results, key = { it.id }) { channel ->
                    // Logic to display appropriate card based on type
                    if (channel.type == StreamType.MOVIE || channel.type == StreamType.SERIES) {
                        MovieCard(
                            channel = channel,
                            imageLoader = imageLoader,
                            onClick = { onChannelClick(channel) }
                        )
                    } else {
                        // Reusing the MovieCard style for TV results in search looks cleaner than mixing aspect ratios
                        // Or we can create a simplified mini-card. For now, MovieCard works well for lists.
                        MovieCard(
                            channel = channel,
                            imageLoader = imageLoader,
                            onClick = { onChannelClick(channel) }
                        )
                    }
                }
            }
        }
    }
}