package com.gcancino.levelingup.presentation.player.identity

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.identity.DailyStandardEntry
import com.gcancino.levelingup.domain.models.identity.IdentityScoreColor
import com.gcancino.levelingup.domain.models.identity.StandardType

@ExperimentalMaterial3Api
@Composable
fun StandardsScreen(
    viewModel: IdentityViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val profile       by viewModel.identityProfile.collectAsStateWithLifecycle()
    val score         by viewModel.todayScore.collectAsStateWithLifecycle()
    val allEntries    by viewModel.todayEntries.collectAsStateWithLifecycle()
    val pendingEntries by viewModel.pendingEntries.collectAsStateWithLifecycle()

    val scoreColor = when (score.color) {
        IdentityScoreColor.PERFECT -> Color(0xFF00E676)
        IdentityScoreColor.HIGH    -> Color(0xFF69F0AE)
        IdentityScoreColor.MEDIUM  -> Color(0xFFFFD740)
        IdentityScoreColor.LOW     -> Color(0xFFFF6E40)
        IdentityScoreColor.NONE    -> Color(0xFFE53935)
        else                       -> Color.Gray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Estándares", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            profile?.identityStatement ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward,  // back icon
                            contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Score del día ─────────────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${(score.overall * 100).toInt()}% de alineación",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = scoreColor
                        )
                        Text(
                            score.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        "${score.completedStandards}/${score.totalStandards}",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Estándares pendientes ─────────────────────────────────────────────
            if (pendingEntries.isNotEmpty()) {
                Text(
                    "PENDIENTES",
                    style         = MaterialTheme.typography.labelLarge,
                    color         = Color.Gray,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(bottom = 8.dp)
                )
                pendingEntries.forEach { entry ->
                    SwipeToCompleteStandardCard(
                        entry      = entry,
                        onComplete = { viewModel.completeStandard(entry.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Estándares completados ────────────────────────────────────────────
            val completed = allEntries.filter { it.isCompleted }
            if (completed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "COMPLETADOS",
                    style         = MaterialTheme.typography.labelLarge,
                    color         = Color.Gray,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(bottom = 8.dp)
                )
                completed.forEach { entry ->
                    CompletedStandardCard(entry = entry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * SwipeToCompleteStandardCard
  */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToCompleteStandardCard(
    entry: DailyStandardEntry,
    onComplete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            // Solo completar estándares manuales — TRAINING se auto-valida
            if (entry.standardType != StandardType.TRAINING) {
                onComplete()
            }
        }
    }

    SwipeToDismissBox(
        state                      = dismissState,
        enableDismissFromStartToEnd = entry.standardType != StandardType.TRAINING,
        enableDismissFromEndToStart = false,
        backgroundContent          = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Color(0xFF43A047) else Color.Transparent,
                label = "swipeBg"
            )
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+${entry.xpAwarded.takeIf { it > 0 } ?: "XP"}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) {
        StandardCard(entry = entry, isCompleted = false)
    }
}

@Composable
private fun CompletedStandardCard(entry: DailyStandardEntry) {
    StandardCard(entry = entry, isCompleted = true)
}

@Composable
private fun StandardCard(entry: DailyStandardEntry, isCompleted: Boolean) {
    val typeColor = entry.standardType.color()

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFF1C2C1C) else Color(0xFF1C1C1E)
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp))
                } else {
                    Icon(entry.standardType.icon(), null, tint = typeColor,
                        modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.standardTitle,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color      = if (isCompleted) Color.Gray else Color.White
                )
                Text(
                    buildString {
                        if (entry.autoValidated) append("Auto-validado · ")
                        if (isCompleted) append("+${entry.xpAwarded} XP ganados")
                        else append("Desliza para completar")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) Color(0xFF00E676) else Color.Gray
                )
            }

            if (entry.standardType == StandardType.TRAINING && !isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Auto",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = typeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}