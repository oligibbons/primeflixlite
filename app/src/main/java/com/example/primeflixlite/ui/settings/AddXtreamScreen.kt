package com.example.primeflixlite.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.primeflixlite.ui.components.NeonTextField
import com.example.primeflixlite.ui.theme.BurntYellow
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.VoidBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun AddXtreamScreen(
    onPlaylistAdded: () -> Unit,
    viewModel: AddXtreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Reset effect if successfully added
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPlaylistAdded()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 100.dp, vertical = 40.dp), // Wide padding for TV look
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect Xtream Account",
            style = MaterialTheme.typography.headlineMedium,
            color = White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        NeonTextField(
            value = viewModel.serverUrl,
            onValueChange = { viewModel.serverUrl = it },
            label = "Server URL (http://...)",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        NeonTextField(
            value = viewModel.username,
            onValueChange = { viewModel.username = it },
            label = "Username",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        NeonTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = "Password",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { viewModel.validateAndSave() },
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = BurntYellow, // THEME UPDATE: Primary Yellow for Action
                contentColor = VoidBlack,     // Black text on Yellow for industrial contrast
                disabledContainerColor = Color.DarkGray
            ),
            modifier = Modifier
                .fillMaxWidth(0.5f) // Not too wide on TV
                .height(50.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = VoidBlack, modifier = Modifier.size(24.dp))
            } else {
                Text("CONNECT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}