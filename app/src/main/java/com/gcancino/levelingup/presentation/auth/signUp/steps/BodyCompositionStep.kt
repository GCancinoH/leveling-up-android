package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel

@Composable
fun BodyCompositionStep(
    viewModel: BodyCompositionViewModel
) {
    val buttonOnClick: () -> Unit
    val buttonContent: @Composable RowScope.() -> Unit
    val state by viewModel.savedState.collectAsState()

    when (state) {
        is Resource.Success -> {
            buttonOnClick = { viewModel.goToNextStep() }
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next"
                )
            }
        }
        is Resource.Loading -> {
            buttonOnClick = { /* Button is typically disabled when loading, or action does nothing */ }
            buttonContent = {
                CircularProgressIndicator(
                    // Use MaterialTheme colors if available for consistency
                    color = Color.Black, // Example if button container is primary
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            buttonOnClick = { viewModel.saveBodyComposition() }
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Body Composition",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Body composition metrics help trac fitness progress and set realistic goals. " +
                        "Also, other health metrics can be calculated with your muscle mass, body fat " +
                        "& visceral fat.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Body Fat TextField
            OutlinedTextField(
                value = viewModel.fatPercentage,
                onValueChange = { viewModel.onFatPercentageChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = "Body Fat"
                    )
                },
                label = { Text("Body Fat %") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Muscle Mass TextField
            OutlinedTextField(
                value = viewModel.musclePercentage,
                onValueChange = { viewModel.onMusclePercentageChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = "Muscle Mass"
                    )
                },
                label = { Text("Muscle Mass %") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Visceral Fat TextField
            OutlinedTextField(
                value = viewModel.visceralFat,
                onValueChange = { viewModel.onVisceralFatChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = "Visceral Fat"
                    )
                },
                label = { Text("Visceral Fat") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Visceral Fat TextField
            OutlinedTextField(
                value = viewModel.bodyAge,
                onValueChange = { viewModel.onBodyAgeChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = "Body Age"
                    )
                },
                label = { Text("Body Age") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )

        }

        Button(
            onClick = buttonOnClick,
            modifier = Modifier.align(Alignment.BottomEnd),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            buttonContent()
        }
    }
}