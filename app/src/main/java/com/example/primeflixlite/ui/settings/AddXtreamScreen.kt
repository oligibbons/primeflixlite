package com.example.primeflixlite.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.primeflixlite.R
import com.example.primeflixlite.ui.components.LoadingOverlay
import com.example.primeflixlite.ui.components.NeonTextField
import com.example.primeflixlite.ui.details.NeonButton
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack

@Composable
fun AddXtreamScreen(
    onPlaylistAdded: () -> Unit,
    viewModel: AddXtreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPlaylistAdded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_transparent),
                contentDescription = "PrimeFlix+",
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth(),
                alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "CONNECT XTREAM CODES",
                style = MaterialTheme.typography.headlineSmall,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.width(400.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                NeonTextField(
                    value = viewModel.serverUrl,
                    onValueChange = { viewModel.serverUrl = it },
                    label = "Server URL (http://...)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next)
                )

                NeonTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = "Username",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next)
                )

                NeonTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = "Password",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeonButton(
                    text = "CONNECT ACCOUNT",
                    icon = Icons.Default.AddLink,
                    isPrimary = true,
                    // FIX: Changed from addAccount to validateAndSave
                    onClick = { viewModel.validateAndSave() }
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = com.example.primeflixlite.ui.theme.ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            LoadingOverlay(message = "Verifying Credentials...")
        }
    }
}