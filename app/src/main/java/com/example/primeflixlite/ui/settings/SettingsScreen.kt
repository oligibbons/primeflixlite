package com.example.primeflixlite.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.primeflixlite.data.local.entity.Playlist
import com.example.primeflixlite.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0.0" }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(48.dp)
    ) {
        // Left Header
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                tint = NeonBlue,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "SETTINGS",
                style = MaterialTheme.typography.displayMedium,
                color = White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "PrimeFlix Lite v$version",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
            if (uiState.message != null) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = uiState.message ?: "",
                    color = NeonYellow,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.width(48.dp))

        // Right Content
        LazyColumn(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "ACTIVE PLAYLISTS",
                    color = NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState.playlists.isEmpty()) {
                item {
                    Text("No playlists found.", color = Color.Gray)
                }
            } else {
                items(uiState.playlists) { playlist ->
                    PlaylistSettingsRow(
                        playlist = playlist,
                        onSync = { viewModel.syncPlaylist(playlist) },
                        onDelete = { viewModel.deletePlaylist(playlist) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            item {
                Text(
                    "GENERAL",
                    color = NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                SettingsTile(
                    title = "Clear Cache",
                    subtitle = "Free up device storage",
                    icon = Icons.Default.Delete,
                    onClick = { /* TODO */ }
                )
            }

            item {
                SettingsTile(
                    title = "About",
                    subtitle = "Licenses & Info",
                    icon = Icons.Default.Info,
                    onClick = { }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF222222) else OffBlack)
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) NeonYellow else Color.DarkGray, // Yellow Focus
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onSync() }
            .focusable(interactionSource = interactionSource)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = if (isFocused) White else Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = playlist.url,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            RefreshButton(onClick = onSync)
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun RefreshButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isFocused) NeonYellow else Color.Transparent)
            .border(1.dp, if (isFocused) NeonYellow else NeonBlue, RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Sync",
            tint = if (isFocused) VoidBlack else NeonBlue
        )
    }
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .scale(if (isFocused) 1.02f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF222222) else OffBlack)
            .border(
                width = if (isFocused) 3.dp else 0.dp, // High Vis Yellow
                color = if (isFocused) NeonYellow else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) NeonYellow else Color.Gray,
            modifier = Modifier.size(28.dp)
        )

        Spacer(Modifier.width(20.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) White else Color.LightGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}