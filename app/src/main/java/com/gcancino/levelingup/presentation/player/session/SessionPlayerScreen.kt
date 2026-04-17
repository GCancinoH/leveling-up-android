package com.gcancino.levelingup.presentation.player.session

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.exercise.*
import com.gcancino.levelingup.ui.theme.BackgroundColor
import timber.log.Timber
import java.util.Locale

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun SessionPlayerScreen(
    viewModel: SessionPlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val sessionState        by viewModel.session.collectAsState()
    val restTime            by viewModel.restTimeRemaining.collectAsState()
    val restTotal           by viewModel.restTotalSeconds.collectAsState()
    val completedCount      by viewModel.completedSetsCount.collectAsState()
    val totalCount          by viewModel.totalSetsCount.collectAsState()
    val completedSets       by viewModel.completedSets.collectAsState()
    val isSessionComplete   by viewModel.isSessionComplete.collectAsState()
    val plateTarget         by viewModel.plateCalculatorTarget.collectAsState()

    // ── Notification permission rationale ─────────────────────────────────────────
    var showNotificationRationale by remember { mutableStateOf(false) }
    var pendingPermissionRequest  by remember { mutableStateOf(false) }
    // ── Fix: read from ViewModel so flag survives recomposition ──────────────────
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

    // ── Plate calculator bottom sheet ─────────────────────────────────────────────
    plateTarget?.let { targetKg ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest   = { viewModel.closePlateCalculator() },
            sheetState         = sheetState,
            containerColor     = Color(0xFF1C1C1E),
            dragHandle         = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            PlateCalculatorSheet(
                targetKg       = targetKg,
                plates         = viewModel.calculatePlates(targetKg),
                onDismiss      = { viewModel.closePlateCalculator() }
            )
        }
    }

    // ── Session complete bottom sheet ─────────────────────────────────────────────
    if (isSessionComplete) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { /* user must tap Finish */ },
            sheetState       = sheetState,
            containerColor   = Color(0xFF1C1C1E),
            dragHandle       = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            SessionSummarySheet(
                completedSets = completedCount,
                totalSets     = totalCount,
                onFinish      = onNavigateBack
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val title = (sessionState as? Resource.Success)?.data?.name ?: "Session"
                        Text(title, fontWeight = FontWeight.Bold)
                        if (totalCount > 0) {
                            Text(
                                text  = "$completedCount / $totalCount sets",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = Color(0xFF030303),
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF030303),
        bottomBar = {
            // ── Circular rest timer replaces the old linear bar ───────────────────
            AnimatedVisibility(
                visible = restTime > 0,
                enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                CircularRestTimer(
                    secondsRemaining = restTime,
                    totalSeconds     = restTotal,
                    onSkip           = { viewModel.skipRest() }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val resource = sessionState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    resource.data?.let { session ->
                        SessionContent(
                            session          = session,
                            completedSets    = completedSets,
                            viewModel        = viewModel,
                            onRequestNotificationRationale = {
                                if (!hasRequestedPermission) {
                                    viewModel.markPermissionRequested()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        showNotificationRationale = true
                                    }
                                }
                            }
                        )
                    } ?: Text(
                        "No session data",
                        color    = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Error -> {
                    Text(
                        "Error: ${resource.message}",
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// ── Circular Rest Timer ───────────────────────────────────────────────────────────

@Composable
private fun CircularRestTimer(
    secondsRemaining: Int,
    totalSeconds: Int,
    onSkip: () -> Unit
) {
    val progress = if (totalSeconds > 0) secondsRemaining / totalSeconds.toFloat() else 0f

    // Animate the sweep angle smoothly each second
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(durationMillis = 800, easing = LinearEasing),
        label          = "timerProgress"
    )

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color(0xFF1C1C1E),
        tonalElevation  = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier              = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: label
            Column {
                Text(
                    text       = "REST",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color.Gray,
                    letterSpacing = 2.sp
                )
                Text(
                    text       = formatSeconds(secondsRemaining),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
            }

            // Center: circular progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val diameter    = size.minDimension - strokeWidth
                    val topLeft     = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize     = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Track
                    drawArc(
                        color       = Color.DarkGray,
                        startAngle  = -90f,
                        sweepAngle  = 360f,
                        useCenter   = false,
                        topLeft     = topLeft,
                        size        = arcSize,
                        style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress sweep (drains clockwise)
                    drawArc(
                        color       = androidx.compose.ui.graphics.Color(0xFF6C63FF),
                        startAngle  = -90f,
                        sweepAngle  = 360f * animatedProgress,
                        useCenter   = false,
                        topLeft     = topLeft,
                        size        = arcSize,
                        style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text       = "${(animatedProgress * 100).toInt()}%",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color.Gray
                )
            }

            // Right: skip button
            TextButton(
                onClick = onSkip,
                colors  = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip rest")
                    Text(
                        "SKIP",
                        style         = MaterialTheme.typography.labelSmall,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
}

// ── Plate Calculator Sheet ────────────────────────────────────────────────────────

@Composable
private fun PlateCalculatorSheet(
    targetKg: Double,
    plates: List<Pair<Double, Int>>,
    onDismiss: () -> Unit
) {
    val plateColors = mapOf(
        25.0  to Color(0xFFE53935), // red
        20.0  to Color(0xFF1E88E5), // blue
        15.0  to Color(0xFFFFB300), // yellow
        10.0  to Color(0xFF43A047), // green
        5.0   to Color(0xFFFFFFFF), // white
        2.5   to Color(0xFF757575), // grey
        1.25  to Color(0xFFBCAAA4)  // chrome
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "Plate Calculator",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text  = "${String.format(Locale.US, "%.1f", targetKg)} kg total · 20 kg bar",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (plates.isEmpty()) {
            Text(
                text  = "No plates needed — bar weight only",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        } else {
            Text(
                text       = "Per side",
                style      = MaterialTheme.typography.labelMedium,
                color      = Color.Gray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visual bar representation
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bar stub
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(12.dp)
                        .background(Color(0xFF9E9E9E), RoundedCornerShape(2.dp))
                )
                // Plates left-to-right (lightest innermost would be reversed, heaviest first)
                plates.forEach { (kg, count) ->
                    val color  = plateColors[kg] ?: Color.Gray
                    val height = when {
                        kg >= 20.0 -> 56.dp
                        kg >= 10.0 -> 48.dp
                        kg >= 5.0  -> 40.dp
                        else       -> 32.dp
                    }
                    val width = 14.dp
                    repeat(count) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier          = Modifier
                                .width(width)
                                .height(height)
                                .background(color, RoundedCornerShape(3.dp)),
                            contentAlignment  = Alignment.Center
                        ) {
                            Text(
                                text  = if (kg >= 5.0) "${kg.toInt()}" else "$kg",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp
                                ),
                                color = if (kg >= 5.0 && kg != 5.0) Color.White else Color.Black
                            )
                        }
                    }
                }
                // Mirror bar stub
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(12.dp)
                        .background(Color(0xFF9E9E9E), RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text list
            plates.forEach { (kg, count) ->
                val color = plateColors[kg] ?: Color.Gray
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text  = "${kg} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text       = "× $count",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Session Summary Sheet ─────────────────────────────────────────────────────────

@Composable
private fun SessionSummarySheet(
    completedSets: Int,
    totalSets: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trophy icon
        Text(text = "🏆", style = MaterialTheme.typography.displayMedium)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text       = "Session Complete!",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text  = "Great work. You crushed today's session.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Sets Done", value = "$completedSets")
            StatDivider()
            StatItem(label = "Total Sets", value = "$totalSets")
            StatDivider()
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
            fontWeight = FontWeight.ExtraBold,
            color      = Color.White
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.DarkGray)
    )
}

// ── Session Content ───────────────────────────────────────────────────────────────

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun SessionContent(
    session: TrainingSession,
    completedSets: Map<String, Set<Int>>,
    viewModel: SessionPlayerViewModel,
    onRequestNotificationRationale: () -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(items = session.blocks, key = { it.id }) { block ->
            BlockItem(
                block                          = block,
                sessionId                      = session.id,
                completedSets                  = completedSets,
                viewModel                      = viewModel,
                onRequestNotificationRationale = onRequestNotificationRationale
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ── Block ─────────────────────────────────────────────────────────────────────────
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun BlockItem(
    block: ExerciseBlock,
    sessionId: String,
    completedSets: Map<String, Set<Int>>,
    viewModel: SessionPlayerViewModel,
    onRequestNotificationRationale: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text          = block.type.toDisplayName(),
                style         = MaterialTheme.typography.labelLarge,
                color         = MaterialTheme.colorScheme.primary,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (block.sets > 1) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "${block.sets} ROUNDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                block.exercises.forEachIndexed { index, exercise ->
                    ExerciseItem(
                        exercise                       = exercise,
                        block                          = block,
                        sessionId                      = sessionId,
                        completedSets                  = completedSets,
                        viewModel                      = viewModel,
                        onRequestNotificationRationale = onRequestNotificationRationale
                    )
                    if (index < block.exercises.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color    = Color.DarkGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Exercise ──────────────────────────────────────────────────────────────────────

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun ExerciseItem(
    exercise: Exercise,
    block: ExerciseBlock,
    sessionId: String,
    completedSets: Map<String, Set<Int>>,
    viewModel: SessionPlayerViewModel,
    onRequestNotificationRationale: () -> Unit
) {
    // Collecting oneRepMaxes here means this composable recomposes when 1RM loads,
    // which causes calculateWeight() to be called again with fresh data —
    // exactly how the original working version behaved.
    @Suppress("UNUSED_VARIABLE")
    val oneRepMaxes by viewModel.oneRepMaxes.collectAsState()

    Column {
        Text(
            text       = exercise.name,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )

        if (!exercise.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint     = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(exercise.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        exercise.sets.forEachIndexed { index, set ->
            val isCompleted = completedSets[exercise.id]?.contains(index) == true

            SetRow(
                exerciseId                     = exercise.id,
                sessionId                      = sessionId,
                index                          = index,
                set                            = set,
                exerciseName                   = exercise.name,
                block                          = block,
                isCompleted                    = isCompleted,
                viewModel                      = viewModel,
                onRequestNotificationRationale = onRequestNotificationRationale
            )

            if (index < exercise.sets.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ── Set Row — long press to complete, tap weight for plate calculator ─────────────
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun SetRow(
    exerciseId: String,
    sessionId: String,
    index: Int,
    set: ExerciseSet,
    exerciseName: String,
    block: ExerciseBlock,
    isCompleted: Boolean,
    viewModel: SessionPlayerViewModel,
    onRequestNotificationRationale: () -> Unit
) {
    // Called directly — parent ExerciseItem collects oneRepMaxes so this
    // recomposes automatically when 1RM loads, same as the original working code.
    val weightText = viewModel.calculateWeight(exerciseName, set.intensity, set.intensityType)
    val weightKg   = viewModel.calculateWeightKg(exerciseName, set.intensity, set.intensityType)

    // Rest label shown below the row
    val restLabel = remember(block, exerciseId, set.restSeconds) {
        if (block.type == BlockType.Main) {
            if (set.restSeconds > 0) "${set.restSeconds}s rest" else null
        } else {
            val isLastExercise = block.exercises.lastOrNull()?.id == exerciseId
            when {
                isLastExercise && block.restAfterBlock > 0 ->
                    "${block.restAfterBlock}s block rest"
                !isLastExercise && block.restBetweenExercises > 0 ->
                    "${block.restBetweenExercises}s switch rest"
                else -> null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // ── Long press to complete/undo ───────────────────────────────────
                .combinedClickable(
                    onClick      = { /* no-op: intentional, prevents accidental taps */ },
                    onLongClick  = {
                        onRequestNotificationRationale()
                        viewModel.toggleSetCompleted(
                            sessionId      = sessionId,
                            exerciseId     = exerciseId,
                            setIndex       = index,
                            block          = block,
                            setRestSeconds = set.restSeconds
                        )
                    }
                )
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                    else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set number / check indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else Color.DarkGray.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint     = Color.Black
                    )
                } else {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Reps
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "${set.reps} Reps",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = if (isCompleted) Color.Gray else Color.White,
                    fontWeight = FontWeight.Medium
                )
                restLabel?.let {
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            // Weight — tappable to open plate calculator
            Column(
                horizontalAlignment = Alignment.End,
                modifier            = Modifier.then(
                    if (weightKg != null) {
                        Modifier.clickable { viewModel.openPlateCalculator(weightKg) }
                    } else Modifier
                )
            ) {
                Text(
                    text  = if (set.intensityType == IntensityType.PERCENTAGE_1RM) {
                        "${set.intensity.toInt()}% 1RM"
                    } else {
                        "${set.intensity.toInt()} ${set.intensityType.name}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.primary
                )
                if (weightText.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = weightText,
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = if (isCompleted) Color.Gray else Color.White
                        )
                        // Small calculator icon hint when plate calc is available
                        if (weightKg != null && !isCompleted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = "View plates",
                                modifier = Modifier.size(12.dp),
                                tint     = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Block type label ──────────────────────────────────────────────────────────────

private fun BlockType.toDisplayName(): String = when (this) {
    BlockType.Main     -> "MAIN WORK"
    BlockType.SuperSet -> "SUPERSET"
    BlockType.JumpSet  -> "JUMP SET"
    BlockType.BiTuT    -> "BI-TUT"
    BlockType.Circuit  -> "CIRCUIT"
    BlockType.Warmup   -> "WARM UP"
    BlockType.Cooldown -> "COOL DOWN"
}