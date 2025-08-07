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
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Player

@Composable
fun InitScreen(
    viewModel: InitViewModel,
    onSignedIn: () -> Unit,
    onSignInError: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()

    if (userState is Resource.Loading)
        InitialLoadingContent(userState)

    LaunchedEffect(userState) {
        when (val state = userState) {
            is Resource.Success -> onSignedIn()
            is Resource.Error -> onSignInError()
            else -> Unit
        }
    }

}

@Composable
fun InitialLoadingContent(
    userState: Resource<Player>
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp)
        )*/
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        if(userState is Resource.Loading) {
            Text(text = "Checking user status...")
        }
    }
}