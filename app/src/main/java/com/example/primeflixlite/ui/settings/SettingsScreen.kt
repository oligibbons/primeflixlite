package com.example.primeflixlite.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.primeflixlite.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0.0" }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(48.dp)
    ) {
        // LEFT COLUMN: Header & Info
        Column(
            modifier = Modifier
                .weight(0.4f)
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Optimized for Android TV",
                color = DarkGray,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.width(48.dp))

        // RIGHT COLUMN: Actions List
        LazyColumn(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                SettingsTile(
                    title = "Manage Profiles",
                    subtitle = "Edit or remove connected accounts",
                    icon = Icons.Default.Person,
                    onClick = { /* Navigate to profile manager */ }
                )
            }

            item {
                SettingsTile(
                    title = "Clear Cache",
                    subtitle = "Free up space on your device",
                    icon = Icons.Default.Delete,
                    onClick = {
                        // Simple cache clear logic could go here
                        viewModel.clearCache()
                    }
                )
            }

            item {
                SettingsTile(
                    title = "About",
                    subtitle = "Legal Information & Licenses",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun SettingsTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .scale(if (isFocused) 1.02f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color(0xFF222222) else OffBlack)
            .border(
                width = 2.dp,
                color = if (isFocused) NeonBlue else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) NeonBlue else Color.Gray,
            modifier = Modifier.size(32.dp)
        )

        Spacer(Modifier.width(24.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) White else Color.LightGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}