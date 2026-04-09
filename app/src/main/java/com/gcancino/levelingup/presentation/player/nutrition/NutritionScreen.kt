package com.gcancino.levelingup.presentation.player.nutrition

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.gcancino.levelingup.domain.models.nutrition.MacroSummary
import com.gcancino.levelingup.domain.models.nutrition.NutritionAlignment
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import com.gcancino.levelingup.domain.models.nutrition.ProcessingLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val analyzeState by viewModel.analyzeState.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()
    val todayMacros  by viewModel.todayMacros.collectAsState()

    // Photo picker
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.analyzePhoto(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Nutrition", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        ))
                    }) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0A))
            )
        },
        containerColor = Color(0xFF0A0A0A),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    ))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.Black
            ) {
                Icon(Icons.Default.AddAPhoto, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Food", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Analyzing overlay ─────────────────────────────────────────────
            if (analyzeState is NutritionViewModel.AnalyzeState.Analyzing) {
                item { AnalyzingCard() }
            }

            // ── Latest result ─────────────────────────────────────────────────
            val successState = analyzeState as? NutritionViewModel.AnalyzeState.Success
            if (successState != null) {
                item {
                    AnimatedVisibility(
                        visible    = true,
                        enter      = fadeIn() + slideInVertically()
                    ) {
                        NutritionResultCard(
                            entry     = successState.entry,
                            onDismiss = { viewModel.resetAnalyzeState() }
                        )
                    }
                }
            }

            // ── Daily macro summary ───────────────────────────────────────────
            if (todayEntries.isNotEmpty()) {
                item { DailyMacroCard(macros = todayMacros, mealCount = todayEntries.size) }
            }

            // ── Today's meal log ──────────────────────────────────────────────
            if (todayEntries.isNotEmpty()) {
                item {
                    Text(
                        "TODAY'S MEALS",
                        style         = MaterialTheme.typography.labelLarge,
                        color         = MaterialTheme.colorScheme.primary,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                items(todayEntries) { entry ->
                    MealLogCard(entry = entry)
                }
            } else if (analyzeState is NutritionViewModel.AnalyzeState.Idle) {
                item { EmptyNutritionState() }
            }
        }
    }
}

// ─── AnalyzingCard ────────────────────────────────────────────────────────────
@Composable
private fun AnalyzingCard() {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier    = Modifier.size(28.dp),
                color       = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Column {
                Text("Analyzing your meal…", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("Checking identity alignment", style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
            }
        }
    }
}

// ─── NutritionResultCard ──────────────────────────────────────────────────────
@Composable
private fun NutritionResultCard(entry: NutritionEntry, onDismiss: () -> Unit) {
    val alignmentColor = when (entry.alignment) {
        NutritionAlignment.ALIGNED    -> Color(0xFF00E676)
        NutritionAlignment.NEUTRAL    -> Color(0xFFFFD740)
        NutritionAlignment.MISALIGNED -> Color(0xFFFF6E40)
    }
    val alignmentEmoji = when (entry.alignment) {
        NutritionAlignment.ALIGNED    -> "✅"
        NutritionAlignment.NEUTRAL    -> "⚠️"
        NutritionAlignment.MISALIGNED -> "❌"
    }

    Card(
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = alignmentColor.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, alignmentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$alignmentEmoji ${entry.alignment.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = alignmentColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.foodIdentified,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                if (entry.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model          = entry.photoUrl,
                        contentDescription = null,
                        modifier       = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale   = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Macros row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip("${entry.calories} kcal", "Calories", Color.White)
                MacroChip("${entry.proteinG.toInt()}g", "Protein", Color(0xFF7986CB))
                MacroChip("${entry.carbsG.toInt()}g", "Carbs", Color(0xFFFFD740))
                MacroChip("${entry.fatsG.toInt()}g", "Fats", Color(0xFFFF6E40))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2C2C2E))
            Spacer(modifier = Modifier.height(12.dp))

            // Identity verdict
            Text(entry.alignmentReason,
                style = MaterialTheme.typography.bodySmall, color = Color(0xFFCCCCCC))

            if (entry.suggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp),
                    color = alignmentColor.copy(alpha = 0.1f)) {
                    Text("💡 ${entry.suggestion}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = alignmentColor,
                        modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss", color = Color.Gray)
            }
        }
    }
}

// ─── DailyMacroCard ───────────────────────────────────────────────────────────
@Composable
private fun DailyMacroCard(macros: MacroSummary, mealCount: Int) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("TODAY'S TOTAL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("$mealCount meal${if (mealCount != 1) "s" else ""} logged",
                style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                MacroChip("${macros.calories}", "kcal", Color.White)
                MacroChip("${macros.proteinG.toInt()}g", "Protein", Color(0xFF7986CB))
                MacroChip("${macros.carbsG.toInt()}g", "Carbs", Color(0xFFFFD740))
                MacroChip("${macros.fatsG.toInt()}g", "Fats", Color(0xFFFF6E40))
            }
        }
    }
}

// ─── MealLogCard ──────────────────────────────────────────────────────────────
@Composable
private fun MealLogCard(entry: NutritionEntry) {
    val alignmentColor = when (entry.alignment) {
        NutritionAlignment.ALIGNED    -> Color(0xFF00E676)
        NutritionAlignment.NEUTRAL    -> Color(0xFFFFD740)
        NutritionAlignment.MISALIGNED -> Color(0xFFFF6E40)
    }

    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Box(modifier = Modifier.size(8.dp).background(alignmentColor, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.foodIdentified, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
                Text("${entry.calories} kcal · ${entry.proteinG.toInt()}g protein",
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            if (entry.photoUrl.isNotBlank()) {
                AsyncImage(model = entry.photoUrl, contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop)
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────
@Composable
private fun MacroChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
private fun EmptyNutritionState() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🍽️", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Log your first meal", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text("Tap + to take a photo and check\nif it aligns with your identity",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}