package com.gcancino.levelingup.presentation.initialization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InitScreen(
    viewModel: InitViewModel,
    onSignedIn: () -> Unit,
    onSignInError: () -> Unit,
    onNeedsOnboarding: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()

    if (userState is InitViewModel.UserState.Loading)
        InitialLoadingContent()

    LaunchedEffect(userState) {
        when (val state = userState) {
            is InitViewModel.UserState.Ready -> {
                if (state.needsOnboarding) onNeedsOnboarding()
                else onSignedIn()
            }
            is InitViewModel.UserState.Error -> onSignInError()
            else -> Unit
        }
    }
}

@Composable
fun InitialLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Checking user status...")
    }
}