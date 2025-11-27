package com.gcancino.levelingup.ui.components.quests

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.models.VoiceToTextParserState
import java.util.Locale

@ExperimentalMaterial3Api
@Composable
fun QuestStartedScreen(
    questID: String,
    viewModel: QuestStartedViewModel,
    onNavigateBack: () -> Unit,
    onQuestTitleLoaded: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    // Permission launcher
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }
    )

    // Load quest when questID changes
    LaunchedEffect(questID) {
        viewModel.loadQuest(questID)
    }

    // Update title when quest is loaded
    LaunchedEffect(uiState.quest) {
        uiState.quest?.let { quest ->
            onQuestTitleLoaded(quest.title ?: "Unknown Quest")
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // Handle screen on/off
    LaunchedEffect(uiState.keepScreenOn) {
        if (uiState.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Show error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    // Show connection messages
    uiState.connectionMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.dismissConnectionMessage()
        }
    }

    // Handle quest saved
    LaunchedEffect(uiState.questSaved) {
        if (uiState.questSaved) {
            Toast.makeText(context, "Quest completed and saved!", Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    // Show loading state while quest is being fetched
    if (uiState.quest == null && !uiState.isLoading && uiState.error == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00D4AA))
        }
        return
    }

    // Show error state if quest couldn't be loaded
    if (uiState.quest == null && uiState.error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Failed to load quest",
                    color = Color(0xFFE74C3C),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D4AA)
                    )
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Main content - only render when quest is available
    uiState.quest?.let { quest ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Connection Status Indicator
            ConnectionStatusBar(
                isOnline = uiState.isOnline,
                onForceOffline = { viewModel.forceOfflineMode() },
                onForceOnline = { viewModel.forceOnlineMode() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Display with animations
            TimerDisplay(
                timeElapsed = uiState.timeElapsed,
                targetTime = uiState.targetTime,
                isTimerRunning = uiState.isTimerRunning,
                isNearTarget = uiState.isNearTarget,
                isOverTarget = uiState.isOverTarget
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quest Details Section
            if (quest.details?.isStrengthQuest() == true) {
                StrengthQuestCard(
                    targetTime = quest.details.targetTime
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Voice Control Section
            VoiceControlSection(
                canRecord = uiState.canRecord,
                voiceState = voiceState,
                lastCommand = uiState.lastVoiceCommand,
                onRequestPermission = {
                    recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            ActionButtons(
                isTimerRunning = uiState.isTimerRunning,
                onStartStop = {
                    if (uiState.isTimerRunning) {
                        viewModel.stopTimer()
                    } else {
                        viewModel.startTimer()
                    }
                },
                onReset = { viewModel.resetTimer() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConnectionStatusBar(
    isOnline: Boolean,
    onForceOffline: () -> Unit,
    onForceOnline: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isOnline) Color(0xFF1A2A1A) else Color(0xFF2A2A1A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isOnline) Color(0xFF00D4AA) else Color(0xFFFFB347)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isOnline) Color(0xFF00D4AA) else Color(0xFFFFB347),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Online Voice Recognition" else "Offline Voice Recognition",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isOnline) Color(0xFF00D4AA) else Color(0xFFFFB347)
                )
            }

            // Debug buttons (remove in production)
            Row {
                TextButton(
                    onClick = onForceOffline,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFFB347)
                    )
                ) {
                    Text("Offline", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = onForceOnline,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF00D4AA)
                    )
                ) {
                    Text("Online", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    timeElapsed: Long,
    targetTime: Long?,
    isTimerRunning: Boolean,
    isNearTarget: Boolean,
    isOverTarget: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer_animations")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_offset"
    )

    Surface(
        modifier = Modifier
            .size(200.dp)
            .offset(
                x = if (isNearTarget) shakeOffset.dp else 0.dp
            ),
        color = Color(0xFF1A1A1A),
        shape = CircleShape,
        border = BorderStroke(
            width = 3.dp,
            color = when {
                isOverTarget -> Color(0xFF00FF7F).copy(alpha = pulseAlpha)
                isNearTarget -> Color(0xFFFFD700).copy(alpha = pulseAlpha)
                isTimerRunning -> Color(0xFF00D4AA)
                else -> Color(0xFF404040)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTime(timeElapsed),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = when {
                    isOverTarget -> Color(0xFF00FF7F)
                    isNearTarget -> Color(0xFFFFD700)
                    isTimerRunning -> Color(0xFF00D4AA)
                    else -> Color.White
                }
            )

            Text(
                text = when {
                    isOverTarget -> "GREAT JOB!"
                    isNearTarget -> "ALMOST THERE!"
                    isTimerRunning -> "RUNNING"
                    else -> "STOPPED"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = if (isOverTarget || isNearTarget) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isOverTarget -> Color(0xFF00FF7F)
                    isNearTarget -> Color(0xFFFFD700)
                    else -> Color(0xFFB0B0B0)
                }
            )

            // Progress indicator for target time
            targetTime?.let { target ->
                Spacer(modifier = Modifier.height(8.dp))
                val progress = (timeElapsed.toFloat() / target).coerceAtMost(1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp),
                    color = when {
                        isOverTarget -> Color(0xFF00FF7F)
                        isNearTarget -> Color(0xFFFFD700)
                        else -> Color(0xFF00D4AA)
                    },
                    trackColor = Color(0xFF404040),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
            }
        }
    }
}

@Composable
private fun StrengthQuestCard(targetTime: Int?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2A1A2A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF9B59B6))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFF9B59B6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STRENGTH QUEST",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF9B59B6)
                )
            }

            targetTime?.let { target ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Target: $target seconds",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun VoiceControlSection(
    canRecord: Boolean,
    voiceState: VoiceToTextParserState,
    lastCommand: String?,
    onRequestPermission: () -> Unit
) {
    if (!canRecord) {
        PermissionDeniedCard(onRequestPermission = onRequestPermission)
    } else {
        Column {
            VoiceStatusCard(
                voiceState = voiceState,
                lastCommand = lastCommand
            )
            Spacer(modifier = Modifier.height(20.dp))
            VoiceCommandsCard()
        }
    }
}

@Composable
private fun PermissionDeniedCard(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF2D1B1B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE74C3C))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE74C3C),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Microphone permission required for voice control",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFAAAA),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE74C3C)
                )
            ) {
                Text("Grant Permission", color = Color.White)
            }
        }
    }
}

@Composable
private fun VoiceStatusCard(
    voiceState: VoiceToTextParserState,
    lastCommand: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (voiceState.isSpeaking) Color(0xFF00D4AA) else Color(0xFF404040)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (voiceState.isSpeaking) Color(0xFF00D4AA) else Color(0xFF404040),
                            shape = CircleShape
                        )
                        .then(
                            if (voiceState.isSpeaking) {
                                Modifier.animateContentSize()
                            } else Modifier
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (voiceState.isSpeaking) "LISTENING..." else "READY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (voiceState.isSpeaking) Color(0xFF00D4AA) else Color(0xFFB0B0B0)
                )
            }

            // Show last recognized command
            lastCommand?.let { command ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last command: $command",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF00D4AA)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (voiceState.isSpeaking) {
                LinearProgressIndicator(
                    progress = {
                        (voiceState.voiceLevel + 20f) / 40f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF00D4AA),
                    trackColor = Color(0xFF404040),
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (voiceState.partialText.isNotEmpty() || voiceState.spokenText.isNotEmpty()) {
                Text(
                    text = "\"${voiceState.partialText.ifEmpty { voiceState.spokenText }}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            }

            voiceState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE74C3C),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VoiceCommandsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A2A1A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "VOICE COMMANDS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF00D4AA)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val commands = listOf(
                "\"Start\" - Begin quest timer",
                "\"Stop\" - End quest timer",
                "\"Pause\" - Pause timer",
                "\"Resume\" - Resume timer"
            )

            commands.forEach { command ->
                Text(
                    text = "• $command",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isTimerRunning: Boolean,
    onStartStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartStop,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTimerRunning) Color(0xFFE74C3C) else Color(0xFF00D4AA)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isTimerRunning) "STOP" else "START",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFB0B0B0)
            ),
            border = BorderStroke(1.dp, Color(0xFF404040)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "RESET",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// Top bar component for your Navigation Scaffold


private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) {
        String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
    }
}