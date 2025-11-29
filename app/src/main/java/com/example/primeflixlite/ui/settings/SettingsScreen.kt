package com.example.primeflixlite.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.ui.theme.BurntYellow
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(40.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Text("Settings & Profiles", style = MaterialTheme.typography.displaySmall, color = White)
        }

        // Message Toast (Simplified)
        if (uiState.message != null) {
            Text(
                text = uiState.message!!,
                color = BurntYellow,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LaunchedEffect(uiState.message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessage()
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(color = NeonBlue, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }

        // Playlist Management List
        Text("Manage Playlists", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            items(uiState.playlists) { playlist ->
                PlaylistSettingsRow(
                    playlist = playlist,
                    onSync = { viewModel.syncPlaylist(playlist) },
                    onDelete = { viewModel.deletePlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
fun PlaylistSettingsRow(
    playlist: Playlist,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) NeonBlue else Color(0xFF333333)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(12.dp))
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable() // The row itself is focusable container
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(playlist.title, color = White, style = MaterialTheme.typography.titleLarge)
            Text(playlist.url, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }

        Row {
            SettingsButton(icon = Icons.Default.Refresh, label = "Sync", onClick = onSync)
            Spacer(Modifier.width(16.dp))
            SettingsButton(icon = Icons.Default.Delete, label = "Delete", color = Color.Red, onClick = onDelete)
        }
    }
}

@Composable
fun SettingsButton(icon: ImageVector, label: String, color: Color = NeonBlue, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bg = if (isFocused) color.copy(alpha = 0.2f) else Color.Transparent
    val border = if (isFocused) color else Color.Gray

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(icon, contentDescription = null, tint = if (isFocused) color else Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (isFocused) color else Color.Gray)
    }
}