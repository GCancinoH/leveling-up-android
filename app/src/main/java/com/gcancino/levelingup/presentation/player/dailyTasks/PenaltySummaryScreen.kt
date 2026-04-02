package com.gcancino.levelingup.presentation.player.dailyTasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltySummary

@Composable
fun PenaltySummaryScreen(
    penalty: PenaltySummary,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0A0A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Penalty Applied",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color      = Color(0xFFE53935)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You had ${penalty.incompleteTasks} incomplete task(s) yesterday.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PenaltyStatItem(label = "XP Lost", value = "-${penalty.xpLost}", color = Color(0xFFE53935))
            PenaltyStatItem(label = "Streak Reset", value = "-${penalty.streakLost}", color = Color(0xFFFF6F00))
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Use today to reclaim your ground. Don't let it happen again.",
            style     = MaterialTheme.typography.bodySmall,
            color     = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Text("I Understand — Let's Go", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun PenaltyStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}