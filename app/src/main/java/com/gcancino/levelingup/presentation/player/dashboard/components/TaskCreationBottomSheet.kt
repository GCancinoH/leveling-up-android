package com.gcancino.levelingup.presentation.player.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.presentation.player.dailyTasks.shared.TaskChip

@Composable
fun TaskCreationBottomSheet(
    tasks: List<DailyTask>,
    canAddMore: Boolean,
    onAddTask: (title: String, priority: TaskPriority) -> Unit,
    onRemoveTask: (String) -> Unit,
    onSave: () -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LOW) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Tomorrow's Tasks",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Text(
            "Focus on what matters. High priority tasks grant more XP.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

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
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        selected = selectedPriority == priority,
                        onClick  = { selectedPriority = priority },
                        label = {
                            Text(
                                "${priority.text}\n+${priority.xpReward} XP",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 14.sp
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
                        onAddTask(newTaskTitle.trim(), selectedPriority)
                        newTaskTitle = ""
                        selectedPriority = TaskPriority.LOW
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSave() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB39DDB))
        ) {
            Text("Save Daily Tasks", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold)
        }
    }
}
