package com.gcancino.levelingup.presentation.player.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.domain.models.player.Attributes
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    attributes: Attributes,
    modifier: Modifier = Modifier,
    maxValue: Float = 100f
) {
    val stats = listOf(
        "STR" to (attributes.strength?.toFloat() ?: 0f),
        "END" to (attributes.endurance?.toFloat() ?: 0f),
        "INT" to (attributes.intelligence?.toFloat() ?: 0f),
        "MOB" to (attributes.mobility?.toFloat() ?: 0f),
        "HEA" to (attributes.health?.toFloat() ?: 0f),
        "FIN" to (attributes.finance?.toFloat() ?: 0f)
    )

    val numAxes = stats.size
    val angleBetweenAxes = 360f / numAxes
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            // 1. Draw Background Web (Concentric Polygons)
            val numLevels = 4
            for (level in 1..numLevels) {
                val levelRadius = radius * (level.toFloat() / numLevels)
                val path = Path()
                for (i in 0 until numAxes) {
                    val angle = Math.toRadians((i * angleBetweenAxes - 90).toDouble())
                    val x = centerX + levelRadius * cos(angle).toFloat()
                    val y = centerY + levelRadius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color.Gray.copy(alpha = 0.2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Draw Axes Lines
            for (i in 0 until numAxes) {
                val angle = Math.toRadians((i * angleBetweenAxes - 90).toDouble())
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(centerX, centerY),
                    end = Offset(x, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Draw Data Polygon
            val dataPath = Path()
            for (i in 0 until numAxes) {
                val value = stats[i].second.coerceAtMost(maxValue)
                val angle = Math.toRadians((i * angleBetweenAxes - 90).toDouble())
                val dataRadius = radius * (value / maxValue)
                val x = centerX + dataRadius * cos(angle).toFloat()
                val y = centerY + dataRadius * sin(angle).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            if (numAxes > 0) {
                dataPath.close()
                // Fill with gradient-like transparency
                drawPath(path = dataPath, color = primaryColor.copy(alpha = 0.3f))
                // Glowing Stroke
                drawPath(path = dataPath, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}
