package com.gcancino.levelingup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TappableCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = listOf(Color(0xFF0f2027), Color(0xFF203A43), Color(0xFF2C5364))
    val cardSelectedBorder =
        if (isSelected)
            BorderStroke(1.dp, brush = Brush.horizontalGradient(gradientColors))
        else BorderStroke(1.dp, Color.Black)
    val checkboxIcon = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        border = cardSelectedBorder,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Row(
            modifier = modifier.fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text)
            Icon(imageVector = checkboxIcon, contentDescription = null)
        }
    }
}