package com.gcancino.levelingup.presentation.player.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.domain.models.player.Attributes

@Composable
fun StatsCard(
    attributes: Attributes,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Player Attributes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            RadarChart(
                attributes = attributes,
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally),
                maxValue = 100f
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Optional: Grid of numerical values
            AttributeGrid(attributes)
        }
    }
}

@Composable
private fun AttributeGrid(attributes: Attributes) {
    val stats = listOf(
        "Strength" to attributes.strength,
        "Endurance" to attributes.endurance,
        "Intelligence" to attributes.intelligence,
        "Mobility" to attributes.mobility,
        "Health" to attributes.health,
        "Finance" to attributes.finance
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        stats.chunked(2).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowStats.forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$label: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = (value ?: 0).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
