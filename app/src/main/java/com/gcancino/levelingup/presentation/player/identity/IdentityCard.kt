package com.gcancino.levelingup.presentation.player.identity

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.domain.models.identity.*
import com.gcancino.levelingup.ui.theme.SystemColors
import androidx.core.graphics.toColorInt

@Composable
fun IdentityCard(
    modifier: Modifier = Modifier,
    viewModel: IdentityViewModel = hiltViewModel(),
    onViewStandards: () -> Unit,
    onSetupIdentity: () -> Unit
) {
    val profile by viewModel.identityProfile.collectAsState()
    val score by viewModel.todayScore.collectAsState()
    val pendingEntries by viewModel.pendingEntries.collectAsState()
    val allEntries by viewModel.todayEntries.collectAsState()

    if (profile == null) {
        IdentitySetupCTA(onSetupIdentity = onSetupIdentity)
        return
    }

    val scoreColor by animateColorAsState(
        targetValue = when (score.color) {
            IdentityScoreColor.PERFECT  -> Color(0xFF00E676)
            IdentityScoreColor.HIGH     -> Color(0xFF69F0AE)
            IdentityScoreColor.MEDIUM   -> Color(0xFFFFD740)
            IdentityScoreColor.LOW      -> Color(0xFFFF6E40)
            IdentityScoreColor.NONE     -> Color(0xFFE53935)
            IdentityScoreColor.NEUTRAL  -> Color.Gray
        },
        animationSpec = tween(500),
        label         = "scoreColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue   = score.overall,
        animationSpec = tween(800),
        label         = "identityProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = SystemColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {

            // ── Header ────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = "IDENTIDAD",
                        style         = MaterialTheme.typography.labelLarge,
                        color         = MaterialTheme.colorScheme.primary,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text       = profile!!.identityStatement,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                // Score circular
                Box(
                    modifier         = Modifier
                        .size(64.dp)
                        .background(scoreColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "${(score.overall * 100).toInt()}%",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = scoreColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score label
            Text(
                text  = score.label,
                style = MaterialTheme.typography.bodySmall,
                color = scoreColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Progress bar de identidad ─────────────────────────────────────────
            LinearProgressIndicator(
                progress  = { animatedProgress },
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color      = scoreColor,
                trackColor = Color(0xFF2C2C2E)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text  = "${score.completedStandards} de ${score.totalStandards} estándares cumplidos hoy",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            // ── Score por rol ─────────────────────────────────────────────────────
            if (score.byRole.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                score.byRole.values.forEach { roleScore ->
                    RoleScoreRow(roleScore = roleScore)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Estándares pendientes (preview top 2) ─────────────────────────────
            if (pendingEntries.isNotEmpty()) {
                pendingEntries.take(2).forEach { entry ->
                    StandardEntryRow(entry = entry)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (allEntries.isNotEmpty()) {
                // Todos completados
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Viviste tu identidad hoy 🔥",
                        style  = MaterialTheme.typography.bodyMedium,
                        color  = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── CTA ───────────────────────────────────────────────────────────────
            Button(
                onClick  = onViewStandards,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.PrimaryColor,
                    contentColor   = Color.Black
                )
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "VER MIS ESTÁNDARES",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── RoleScoreRow ─────────────────────────────────────────────────────────────────

@Composable
private fun RoleScoreRow(roleScore: RoleScore) {
    val roleColor = try {
        Color(roleScore.roleColor.toColorInt())
    } catch (e: Exception) {
        Color(0xFF7986CB)
    }

    val animatedPct by animateFloatAsState(
        targetValue   = roleScore.percentage,
        animationSpec = tween(600),
        label         = "roleScore_${roleScore.roleId}"
    )

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = roleScore.roleName,
            style    = MaterialTheme.typography.labelMedium,
            color    = Color.White,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress   = { animatedPct },
            modifier   = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape),
            color      = roleColor,
            trackColor = Color(0xFF2C2C2E)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text  = "${(roleScore.percentage * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = roleColor,
            modifier = Modifier.width(36.dp)
        )
    }
}

// ─── StandardEntryRow ─────────────────────────────────────────────────────────────

@Composable
private fun StandardEntryRow(entry: DailyStandardEntry) {
    val typeIcon  = entry.standardType.icon()
    val typeColor = entry.standardType.color()

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(typeColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = typeIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text     = entry.standardTitle,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text  = "+${entry.xpAwarded.takeIf { it > 0 } ?: "??"} XP",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun IdentitySetupCTA(onSetupIdentity: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSetupIdentity() },
        shape  = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚡", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Define Your Identity",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Who are you choosing to become?\nSet your identity and daily standards.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick  = { onSetupIdentity() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape    = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SET MY IDENTITY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Extension helpers ────────────────────────────────────────────────────────────
@Composable
fun StandardType.icon(): Painter = when (this) {
    StandardType.TRAINING  -> rememberVectorPainter(Icons.Default.FitnessCenter)
    StandardType.NUTRITION -> rememberVectorPainter(Icons.Default.Restaurant)
    StandardType.SLEEP     -> rememberVectorPainter(Icons.Default.Bedtime)
    StandardType.MINDSET   -> painterResource(id = com.gcancino.levelingup.R.drawable.brain) // Your custom drawable
    StandardType.DEEP_WORK -> rememberVectorPainter(Icons.Default.Computer)
    StandardType.LEARNING  -> rememberVectorPainter(Icons.Default.AutoStories)
    StandardType.FINANCE   -> rememberVectorPainter(Icons.Default.AttachMoney)
    StandardType.CUSTOM    -> rememberVectorPainter(Icons.Default.Star)
}

fun StandardType.color(): Color = when (this) {
    StandardType.TRAINING  -> Color(0xFF4FC3F7) // Electric Blue
    StandardType.NUTRITION -> Color(0xFF66BB6A) // Green
    StandardType.SLEEP     -> Color(0xFF5C6BC0) // Indigo
    StandardType.MINDSET   -> Color(0xFFBB86FC) // Purple
    StandardType.DEEP_WORK -> Color(0xFFFF7043) // Orange
    StandardType.LEARNING  -> Color(0xFF26A69A) // Teal
    StandardType.FINANCE   -> Color(0xFFFFCA28) // Amber
    StandardType.CUSTOM    -> Color(0xFF9E9E9E) // Gr
}