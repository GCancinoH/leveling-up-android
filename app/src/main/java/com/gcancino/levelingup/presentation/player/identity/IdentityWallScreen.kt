package com.gcancino.levelingup.presentation.player.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.data.local.database.entities.WeeklyReportEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityWallScreen(
    viewModel: IdentityWallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val profile       by viewModel.profile.collectAsState()
    val weeklyReports by viewModel.weeklyReports.collectAsState()
    val streak        by viewModel.currentStreak.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Identity Wall", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Identity declaration ──────────────────────────────────────────
            item {
                profile?.let {
                    IdentityDeclarationCard(
                        statement = it.identityStatement,
                        roles     = it.roles.map { r -> r.name },
                        streak    = streak
                    )
                }
            }

            // ── Weekly reports ────────────────────────────────────────────────
            if (weeklyReports.isNotEmpty()) {
                item {
                    Text(
                        "WEEKLY MIRROR",
                        style         = MaterialTheme.typography.labelLarge,
                        color         = MaterialTheme.colorScheme.primary,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                items(weeklyReports) { report ->
                    WeeklyReportCard(report = report)
                }
            } else {
                item { EmptyReportCard() }
            }
        }
    }
}

@Composable
private fun WeeklyReportCard(report: WeeklyReportEntity) {
    var expanded by remember { mutableStateOf(false) }

    val scoreColor = when {
        report.overallScore >= 0.8f -> Color(0xFF00E676)
        report.overallScore >= 0.5f -> Color(0xFFFFD740)
        else                        -> Color(0xFFFF6E40)
    }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

            // Header row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Week of ${report.weekStart}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        report.headline,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .background(scoreColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(report.overallScore * 100).toInt()}%",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = scoreColor
                    )
                }
            }

            // Role pills
            if (report.strongestRole.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RolePill(label = "Best: ${report.strongestRole}", color = Color(0xFF00E676))
                    RolePill(label = "Needs: ${report.weakestRole}", color = Color(0xFFFF6E40))
                }
            }

            // Expand button
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (expanded) "Show less" else "Read full report",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Expanded content
            if (expanded) {
                HorizontalDivider(color = Color(0xFF2C2C2E))
                Spacer(modifier = Modifier.height(12.dp))
                ReportSection("🔍 Pattern", report.patternIdentified)
                Spacer(modifier = Modifier.height(8.dp))
                ReportSection("🪞 Mirror", report.mirrorInsight)
                Spacer(modifier = Modifier.height(8.dp))
                ReportSection("⚡ One Correction", report.oneCorrection)
                Spacer(modifier = Modifier.height(8.dp))
                ReportSection("🎯 Identity Alignment", report.identityAlignment)
            }
        }
    }
}

@Composable
private fun IdentityDeclarationCard(
    statement: String,
    roles: List<String>,
    streak: Int
) {
    Card(
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚡", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                statement,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                textAlign  = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Roles chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { role ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            role,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF2C2C2E))
            Spacer(modifier = Modifier.height(16.dp))

            // Streak
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("🔥", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$streak day streak",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }
    }
}

@Composable
private fun ReportSection(label: String, content: String) {
    Text(label, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(content, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCCCCCC))
}

@Composable
private fun RolePill(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun EmptyReportCard() {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Your weekly report is being prepared",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Complete your daily standards this week.\nEvery Sunday, the Mirror analyzes your patterns.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}