package com.gcancino.levelingup.presentation.player.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.logic.LevelCalculator
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.ui.theme.BackgroundColor
import com.gcancino.levelingup.R
import com.gcancino.levelingup.presentation.player.profile.components.StatsCard
import timber.log.Timber

@ExperimentalMaterial3Api
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToIdentityWall: () -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val playerDataState by viewModel.playerData.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {/* TODO() */}) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Sync")
                    }
                    IconButton(onClick = { onNavigateToIdentityWall() }) {
                        Icon(Icons.Default.Psychology, contentDescription = "Identity Wall")
                    }
                    IconButton(onClick = { viewModel.signOut(onSignOut) }) {
                        Icon(painter = painterResource(R.drawable.logout), contentDescription = "Sign out")
                    }
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val resource = playerDataState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is Resource.Success -> {
                    val data = resource.data
                    if (data != null) {
                        Timber.tag("PlayerScreen").d("PlayerData: $data")
                        ProfileContent(
                            data = data,
                            scrollState = scrollState,
                        )
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = "Error loading profile",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    data: PlayerData,
    scrollState: androidx.compose.foundation.ScrollState,
) {
    Timber.d("PlayerData: player=${data.player}, attributes=${data.attributes}")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFF2C2C2E)
            ) {
                if (data.player?.photoURL != null) {
                    AsyncImage(
                        model = data.player.photoURL,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                } else {
                    // Placeholder Initials or Icon
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = data.player?.displayName?.firstOrNull()?.uppercase() ?: "P",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            FloatingActionButton(
                onClick = { /* TODO: Edit Photo */ },
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = data.player?.displayName ?: "Player Name",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Level Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            val totalXp = data.progress?.exp ?: 0
            val currentLevel = LevelCalculator.calculateLevel(totalXp)
            val progress = LevelCalculator.calculateProgress(totalXp)
            val xpToNext = LevelCalculator.xpToNextLevel(totalXp)
            val nextLevelXp = LevelCalculator.xpRequiredForLevel(currentLevel + 1)

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Level $currentLevel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "$totalXp / $nextLevelXp XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF2C2C2E)
                )

                if (currentLevel < 100) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$xpToNext XP until next level",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Section
        data.attributes?.let { attributes ->
            StatsCard(attributes = attributes)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
