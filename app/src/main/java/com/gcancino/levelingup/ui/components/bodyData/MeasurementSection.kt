package com.gcancino.levelingup.ui.components.bodyData

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MeasurementSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = Color.DarkGray
    )
    Text(
        text          = title.uppercase(),
        style         = MaterialTheme.typography.labelLarge,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(bottom = 12.dp)
    )
    Column(content = content)
}