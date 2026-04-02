package com.gcancino.levelingup.presentation.player.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.BodyCompositionViewModel
import com.gcancino.levelingup.ui.components.bodyData.BodyDataField
import com.gcancino.levelingup.ui.components.bodyData.UnitToggle

@ExperimentalMaterial3Api
@Composable
fun BodyCompositionBottomSheet(
    viewModel: BodyCompositionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    snackbarState: SnackbarHostState
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val unitSystem by viewModel.unitSystem.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val scope = rememberCoroutineScope()

    // Dismiss automatically on successful save
    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) {
            viewModel.resetSaveState()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Body Composition",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                // Unit toggle
                UnitToggle(
                    unitSystem = unitSystem,
                    onToggle   = { viewModel.toggleUnitSystem() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "All fields are required",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // ── Initial data toggle ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text       = "Baseline entry",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = "Use as reference for progress tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked         = viewModel.isInitialData,
                    onCheckedChange = { viewModel.isInitialData = it },
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor  = Color.White,
                        checkedTrackColor  = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color    = Color.DarkGray
            )

            // ── Fields ────────────────────────────────────────────────────────────
            BodyDataField(
                label       = "Weight",
                value       = viewModel.weight,
                onValueChange = { viewModel.weight = it },
                unit        = viewModel.weightUnit()
            )
            BodyDataField(
                label       = "BMI",
                value       = viewModel.bmi,
                onValueChange = { viewModel.bmi = it },
                unit        = ""
            )
            BodyDataField(
                label       = "Body Fat",
                value       = viewModel.bodyFatPercentage,
                onValueChange = { viewModel.bodyFatPercentage = it },
                unit        = "%"
            )
            BodyDataField(
                label       = "Muscle Mass",
                value       = viewModel.muscleMassPercentage,
                onValueChange = { viewModel.muscleMassPercentage = it },
                unit        = "%"
            )
            BodyDataField(
                label       = "Visceral Fat",
                value       = viewModel.visceralFat,
                onValueChange = { viewModel.visceralFat = it },
                unit        = ""
            )
            BodyDataField(
                label       = "Body Age",
                value       = viewModel.bodyAge,
                onValueChange = { viewModel.bodyAge = it },
                unit        = "yrs"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Error ─────────────────────────────────────────────────────────────
            if (saveState is Resource.Error) {
                Text(
                    text     = (saveState as Resource.Error).message ?: "Error saving",
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── Save button ───────────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.save() },
                enabled  = viewModel.isFormValid && saveState !is Resource.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                if (saveState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        color     = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Save",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}