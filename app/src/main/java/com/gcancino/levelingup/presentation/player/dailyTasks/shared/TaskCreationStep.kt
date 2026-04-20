package com.gcancino.levelingup.presentation.player.dailyTasks.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.domain.models.identity.Objective

@Composable
fun TaskCreationStep(
    tasks: List<DailyTask>,
    weeklyObjectives: List<Objective> = emptyList(),
    canAddMore: Boolean,
    onAddTask: (title: String, priority: TaskPriority, objectiveId: String?) -> Unit,
    onRemoveTask: (String) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LOW) }
    var selectedObjectiveId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Tactical Tomorrow ⚡",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Text(
            "Align tomorrow's tasks with your weekly vision.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        if (weeklyObjectives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Focusing on Weekly Objectives:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeklyObjectives.forEach { objective ->
                    FilterChip(
                        selected = selectedObjectiveId == objective.id,
                        onClick = { selectedObjectiveId = if (selectedObjectiveId == objective.id) null else objective.id },
                        label = { Text(objective.title, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7986CB).copy(alpha = 0.3f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing tasks
        tasks.forEach { task ->
            TaskChip(task = task, onRemove = { onRemoveTask(task.id) })
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (canAddMore) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value         = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label         = { Text("Task title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color(0xFF7986CB),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedLabelColor    = Color(0xFF7986CB),
                    unfocusedLabelColor  = Color.Gray,
                    cursorColor          = Color(0xFF7986CB)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Priority selector
            Text("Priority", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedPriority == priority,
                        onClick  = { selectedPriority = priority },
                        label = {
                            Text(
                                "${priority.text}\n+${priority.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priority.color.copy(alpha = 0.2f),
                            selectedLabelColor = priority.color
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick  = {
                    if (newTaskTitle.isNotBlank()) {
                        onAddTask(newTaskTitle.trim(), selectedPriority, selectedObjectiveId)
                        newTaskTitle = ""
                        selectedPriority = TaskPriority.LOW
                        selectedObjectiveId = null
                    }
                },
                enabled  = newTaskTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7986CB))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Task")
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Maximum 5 tasks reached",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
