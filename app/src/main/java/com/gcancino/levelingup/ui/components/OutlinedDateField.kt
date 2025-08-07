package com.gcancino.levelingup.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedDateField(
    value: Date?,
    onValueChange: (Date) -> Unit,
    error: String?,
    label: String = "Birthday",
    minAge: Int = 16
) {
    val context = LocalContext.current
    var displayText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val maxDate = Calendar.getInstance()
    val minDate = Calendar.getInstance()
    minDate.add(Calendar.YEAR, -80)

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) { showDialog = true }
    }
    LaunchedEffect(value) {
        displayText = value?.let {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(it)
        } ?: ""
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = { /* Readonly */ },
        label = { Text("Date of Birth") },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "Select Date"
            )
        },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Show Date Picker"
                )
            }
        },
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        interactionSource = interactionSource
    )
    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendar.timeInMillis,
        )

        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                calendar.timeInMillis = millis
                onValueChange(calendar.time)
                displayText = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(calendar.time)
            }
        }

        DatePickerDialog(
            modifier = Modifier.padding(16.dp),
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            calendar.timeInMillis = millis
                            onValueChange(calendar.time)
                        }
                        showDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}