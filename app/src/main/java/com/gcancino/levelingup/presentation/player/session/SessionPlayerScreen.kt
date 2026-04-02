package com.gcancino.levelingup.presentation.player.session

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.exercise.*
import com.gcancino.levelingup.ui.theme.BackgroundColor
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun SessionPlayerScreen(
    viewModel: SessionPlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val sessionState        by viewModel.session.collectAsState()
    val completedCount      by viewModel.completedSetsCount.collectAsState()
    val totalCount          by viewModel.totalSetsCount.collectAsState()
    val isSessionComplete   by viewModel.isSessionComplete.collectAsState()

    // ── Notification permission rationale ─────────────────────────────────────────
    var showNotificationRationale by remember { mutableStateOf(false) }
    var pendingPermissionRequest  by remember { mutableStateOf(false) }
    val hasRequestedPermission by viewModel.hasRequestedPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.tag("SessionPlayerScreen").d("Notification permission: $isGranted")
    }

    LaunchedEffect(pendingPermissionRequest) {
        if (pendingPermissionRequest) {
            pendingPermissionRequest = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest  = { showNotificationRationale = false },
            containerColor    = Color(0xFF1C1C1E),
            titleContentColor = Color.White,
            textContentColor  = Color.Gray,
            icon = {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Enable Rest Timer Alerts", fontWeight = FontWeight.Bold) },
            text  = {
                Text("Get notified when your rest period ends so you never miss a set, even when the screen is off.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    pendingPermissionRequest  = true
                }) {
                    Text("Allow", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationale = false }) {
                    Text("Not Now", color = Color.Gray)
                }
            }
        )
    }

    // ── Session complete bottom sheet ─────────────────────────────────────────────
    if (isSessionComplete) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { /* user must tap Finish */ },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1C1C1E),
            dragHandle         = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            SessionSummarySheet(
                completedSets = completedCount,
                totalSets     = totalCount,
                onFinish      = {
                    viewModel.finishSession(onComplete = onNavigateBack)
                }
            )
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            SessionTopBar(
                onBack = onNavigateBack,
                sessionName = (sessionState as? Resource.Success)?.data?.name ?: "Loading..."
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val res = sessionState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is Resource.Error -> {
                    Text(
                        text = res.message ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Success -> {
                    val session = res.data
                    if (session == null) {
                        Text(
                            text = "No session found for today",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        SessionContent(session, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionTopBar(onBack: () -> Unit, sessionName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = sessionName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SessionContent(session: TrainingSession, viewModel: SessionPlayerViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(session.blocks) { block ->
            BlockItem(block, viewModel)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BlockItem(block: ExerciseBlock, viewModel: SessionPlayerViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = block.type.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (block.sets > 1) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${block.sets} ROUNDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                block.exercises.forEachIndexed { index, exercise ->
                    ExerciseItem(exercise, viewModel)
                    if (index < block.exercises.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ExerciseItem(exercise: Exercise, viewModel: SessionPlayerViewModel) {
    Column {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        if (!exercise.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(exercise.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        exercise.sets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.width(40.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${set.reps} Reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                val weightText = viewModel.calculateWeight(exercise.name, set.intensity, set.intensityType)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (set.intensityType == IntensityType.PERCENTAGE_1RM) {
                            "${set.intensity.toInt()}% 1RM"
                        } else {
                            "${set.intensity.toInt()} ${set.intensityType.name}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (weightText.isNotEmpty()) {
                        Text(
                            text = weightText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
                
                if (set.restSeconds > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${set.restSeconds}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummarySheet(
    completedSets: Int,
    totalSets: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            "Another day closer to reaching your peak.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Sets Done", value = "$completedSets/$totalSets")
            StatItem(
                label = "Completion",
                value = if (totalSets > 0) "${(completedSets * 100 / totalSets)}%" else "—"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick  = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                "Finish Session",
                fontWeight = FontWeight.ExtraBold,
                style      = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
    }
}
