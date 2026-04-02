package com.gcancino.levelingup.presentation.player.dailyTasks

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.TasksViewModel
import com.gcancino.levelingup.ui.theme.priorityColor

@ExperimentalMaterial3Api
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val context = LocalContext.current

    // XP toast
    LaunchedEffect(Unit) {
        viewModel.xpEarned.collect { xp ->
            Toast.makeText(context, "+$xp XP earned! 🎯", Toast.LENGTH_SHORT).show()
        }
    }

    // Level up handled by parent via Navigation — emit up if needed
    // For now shown as a toast; replace with full screen in future
    LaunchedEffect(Unit) {
        viewModel.levelUp.collect { newLevel ->
            Toast.makeText(context, "⬆️ LEVEL UP! You're now Level $newLevel!", Toast.LENGTH_LONG).show()
        }
    }

    val completed = allTasks.count { it.isCompleted }
    val total     = allTasks.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's Tasks", fontWeight = FontWeight.Bold, color = Color.White)
                        if (total > 0) {
                            Text(
                                "$completed / $total completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF030303))
            )
        },
        containerColor = Color(0xFF030303)
    ) { padding ->
        if (pendingTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All tasks complete!", style = MaterialTheme.typography.titleMedium,
                        color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Great discipline today.", style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Swipe right to complete",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(items = pendingTasks, key = { it.id }) { task ->
                    SwipeToCompleteTaskCard(
                        task      = task,
                        onComplete = { viewModel.completeTask(task.id) }
                    )
                }
            }
        }
    }
}

// ─── SwipeToCompleteTaskCard ──────────────────────────────────────────────────────

@ExperimentalMaterial3Api
@Composable
private fun SwipeToCompleteTaskCard(
    task: DailyTask,
    onComplete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f }
    )

    // Trigger completion when the swipe state changes to StartToEnd
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            onComplete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Green background revealed on swipe
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Color(0xFF43A047) else Color.Transparent,
                label = "swipeBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "+${task.xpReward} XP",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false
    ) {
        // Task card
        val priorityColor = priorityColor(task.priority)
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(priorityColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${task.priority}",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = priorityColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White
                    )
                    Text(
                        "Complete to earn ${task.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe to complete",
                    tint     = Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
