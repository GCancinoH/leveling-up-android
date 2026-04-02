package com.gcancino.levelingup.ui.components.bodyData

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem

@Composable
fun UnitToggle(
    unitSystem: UnitSystem,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2C2C2E),
        modifier = Modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UnitChip(label = "kg / cm",  selected = unitSystem == UnitSystem.METRIC)
            Spacer(modifier = Modifier.width(2.dp))
            UnitChip(label = "lbs / in", selected = unitSystem == UnitSystem.IMPERIAL)
        }
    }
}