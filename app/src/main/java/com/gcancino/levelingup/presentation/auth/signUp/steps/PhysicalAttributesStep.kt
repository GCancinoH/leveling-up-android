package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
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

@Composable()
fun PhysicalAttributesStep(
    viewModel: PhysicalAttributesViewModel,
) {
    val bmiInterpretationString: String = viewModel.bmiInterpretation
    val state by viewModel.saveState.collectAsState()

    val onButtonClick : () -> Unit
    val buttonContent : @Composable () -> Unit

    when (state) {
        is Resource.Success -> {
            onButtonClick = { viewModel.goToNextStep() }
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Next"
                )
            }
        }
        is Resource.Loading -> {
            onButtonClick = { /* Button is typically disabled when loading, or action does nothing */ }
            buttonContent = {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, // Or Color.Black if that's intended
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        is Resource.Error -> {
            onButtonClick = { viewModel.saveData() } // Or triggerSaveOperation from VM
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }
        } else -> {
            onButtonClick = { viewModel.saveData() } // Or triggerSaveOperation from VM
            buttonContent = {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Physical Attributes",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your physical measurements are essential for tracking progress and calculating health metrics.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.weight,
                onValueChange = { viewModel.onWeightChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.MonitorWeight,
                        contentDescription = "Name"
                    )
                },
                label = { Text("Your Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Height TextField
            OutlinedTextField(
                value = viewModel.height,
                onValueChange = { viewModel.onHeightChange(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Height,
                        contentDescription = "Height"
                    )
                },
                label = { Text("Your Height (m)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // BMI
            OutlinedTextField(
                value = viewModel.bmi,
                onValueChange = { /* TODO() */ },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Scale,
                        contentDescription = "BMI"
                    )
                },
                label = { Text("BMI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                readOnly = true,
                enabled = false,
                supportingText = {

                    val bmiString = "BMI Interpretation: $bmiInterpretationString"
                    if (viewModel.bmi.isNotEmpty()) Text(text = bmiString)
                }
            )
        }

        if (viewModel.isWeightValid() && viewModel.isHeightValid() && viewModel.isBMIValid()) {
            Button(
                onClick = onButtonClick,
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
}