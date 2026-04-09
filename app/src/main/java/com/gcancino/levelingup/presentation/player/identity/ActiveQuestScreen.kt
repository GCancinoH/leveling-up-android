package com.gcancino.levelingup.presentation.player.identity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.identity.GeneratedQuestType

@Composable
fun ActiveQuestCard(
    modifier: Modifier = Modifier,
    viewModel: ActiveQuestViewModel = hiltViewModel()
) {
    val quest by viewModel.activeQuest.collectAsState()
    quest ?: return  // no active quest — show nothing

    val q         = quest!!
    val progress  = q.currentProgress.toFloat() / q.goal.toFloat()
    val questType = try { GeneratedQuestType.valueOf(q.type) } catch (e: Exception) { GeneratedQuestType.CONSISTENCY }

    val typeColor = when (questType) {
        GeneratedQuestType.STREAK      -> Color(0xFFFFD740)
        GeneratedQuestType.CONSISTENCY -> Color(0xFF7986CB)
        GeneratedQuestType.ELIMINATION -> Color(0xFF00E676)
    }

    val typeIcon = when (questType) {
        GeneratedQuestType.STREAK      -> "🔥"
        GeneratedQuestType.CONSISTENCY -> "📊"
        GeneratedQuestType.ELIMINATION -> "🎯"
    }

    val animatedProgress by animateFloatAsState(
        targetValue   = progress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label         = "questProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = typeColor.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, typeColor.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(typeIcon, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CORRECTIVE QUEST",
                            style         = MaterialTheme.typography.labelSmall,
                            color         = typeColor,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        q.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }

                // Progress badge
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .background(typeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${q.currentProgress}/${q.goal}",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = typeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                q.description,
                style   = MaterialTheme.typography.bodySmall,
                color   = Color.Gray,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress   = { animatedProgress },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color      = typeColor,
                trackColor = Color(0xFF2C2C2E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Days remaining
            val daysLeft = try {
                val end   = q.endDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val today = java.time.LocalDate.now()
                java.time.temporal.ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
            } catch (e: Exception) { 0 }

            Text(
                "$daysLeft day${if (daysLeft != 1) "s" else ""} remaining · +${q.xpReward} XP on completion",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}