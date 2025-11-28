package com.gcancino.levelingup.ui.components.quests

import android.app.Activity
import android.speech.tts.TextToSpeech
import android.util.Log
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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.gcancino.levelingup.R
import com.gcancino.levelingup.domain.models.EnduranceDetails
import com.gcancino.levelingup.domain.models.Quests
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestVoiceControlBottomSheet(
    viewModel: QuestStartedViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val uiState by viewModel.uiState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollable = rememberScrollState()

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }
    )

    // Permission handling
    LaunchedEffect(Unit) {
        recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color(0xFF0A0A0A),
        contentColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp),
                color = Color(0xFF404040),
                shape = RoundedCornerShape(2.dp)
            ) {}
        },
        properties = ModalBottomSheetProperties()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollable),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quest Active Header
            /*Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isTimerRunning) {
                                listOf(Color(0xFF00D4AA), Color(0xFF00A693))
                            } else {
                                listOf(Color(0xFF6A4C93), Color(0xFF9B59B6))
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isTimerRunning) Icons.Default.Stop else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTimerRunning) "QUEST IN PROGRESS" else if (canRecord) "VOICE CONTROL READY" else "MICROPHONE PERMISSION NEEDED",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))*/

            // Quest Title
            Text(
                text = uiState.quest?.title ?: "Unknown Quest",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display with animations
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val shakeOffset by infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .offset(
                        x = if (uiState.isNearTarget) shakeOffset.dp else 0.dp
                    ),
                color = Color(0xFF1A1A1A),
                shape = CircleShape,
                border = BorderStroke(
                    width = 3.dp,
                    color = when {
                        uiState.isOverTarget -> Color(0xFF00FF7F).copy(alpha = pulseAlpha) // Bright green for achievement
                        uiState.isNearTarget -> Color(0xFFFFD700).copy(alpha = pulseAlpha)
                        uiState.isTimerRunning -> Color(0xFF00D4AA)
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
                        text = formatTime(uiState.timeElapsed),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = when {
                            uiState.isOverTarget -> Color(0xFF00FF7F) // Bright green for success
                            uiState.isNearTarget -> Color(0xFFFFD700)
                            uiState.isTimerRunning -> Color(0xFF00D4AA)
                            else -> Color.White
                        }
                    )

                    Text(
                        text = when {
                            uiState.isOverTarget -> "GREAT JOB!"
                            uiState.isNearTarget -> "ALMOST THERE!"
                            uiState.isTimerRunning -> "RUNNING"
                            else -> "STOPPED"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = if (uiState.isOverTarget || uiState.isNearTarget) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = when {
                            uiState.isOverTarget -> Color(0xFF00FF7F)
                            uiState.isNearTarget -> Color(0xFFFFD700)
                            else -> Color(0xFFB0B0B0)
                        }
                    )

                    // Progress indicator for target time
                    uiState.targetTime?.let { target ->
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = (uiState.timeElapsed.toFloat() / target).coerceAtMost(1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp),
                            color = when {
                                uiState.isOverTarget -> Color(0xFF00FF7F)
                                uiState.isNearTarget -> Color(0xFFFFD700)
                                else -> Color(0xFF00D4AA)
                            },
                            trackColor = Color(0xFF404040),
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.quest?.details?.isStrengthQuest() == true) {
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

                        uiState.quest?.details?.targetTime?.let { targetTime ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Target: $targetTime seconds",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permission denied message
            if (!uiState.canRecord) {
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
                            onClick = {
                                recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE74C3C)
                            )
                        ) {
                            Text("Grant Permission", color = Color.White)
                        }
                    }
                }
            } else {
                // Voice Status Section
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
                            // Listening indicator
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice level indicator
                        if (voiceState.isSpeaking) {
                            LinearProgressIndicator(
                                progress = {
                                    (voiceState.voiceLevel + 20f) / 40f // Normalize dB level
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

                        // Last recognized text
                        if (voiceState.partialText.isNotEmpty() || voiceState.spokenText.isNotEmpty()) {
                            Text(
                                text = "\"${voiceState.partialText.ifEmpty { voiceState.spokenText }}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0B0B0),
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        // Error display
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

            Spacer(modifier = Modifier.height(20.dp))

            // Voice Commands Guide (only show if permission granted)
            if (uiState.canRecord) {
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

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Manual Controls
                Button(
                    onClick = {
                        if (uiState.isTimerRunning) {
                            viewModel.stopTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isTimerRunning) Color(0xFFE74C3C) else Color(0xFF00D4AA)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (uiState.isTimerRunning) "STOP" else "START",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                // Done Button
                OutlinedButton(
                    onClick = onDismiss,
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
                        text = "DONE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds) else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
    }
}

private fun parseTargetTime(seconds: Int) : Long {
    return seconds.toLong()
}