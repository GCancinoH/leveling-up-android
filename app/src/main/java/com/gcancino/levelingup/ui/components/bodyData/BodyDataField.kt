package com.gcancino.levelingup.ui.components.bodyData

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.nio.file.WatchEvent

@Composable
fun BodyDataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    readOnly: Boolean = false
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { input ->
                // Only allow numeric input with one decimal point
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(input)
                }
            },
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                textAlign = TextAlign.End
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = MaterialTheme.colorScheme.primary
            )
        )
    }
    /*
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    // Only allow numeric input with one decimal point
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        onValueChange(input)
                    }
                },
                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    textAlign = TextAlign.End
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }*/
}