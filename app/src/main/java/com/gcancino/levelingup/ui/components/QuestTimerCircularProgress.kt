package com.gcancino.levelingup.ui.components

import android.R.attr.strokeWidth
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.ui.theme.purpleBlueGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val QUEST_START_TIME = LocalTime.of(7, 0)
private val QUEST_END_TIME = LocalTime.of(23, 59)

@Composable
fun QuestTimerCircularProgressBar(
    modifier: Modifier = Modifier,
    progressBarBrush: Brush = purpleBlueGradient,
    progressBarTrackColor: Color = Color.LightGray.copy(alpha = 0.1f),
    strokeWidth: Dp = 5.dp,
    size: Dp = 80.dp,
    timerUpdateIntervalMillis: Long = 1000L
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        flow {
            while (true) {
                emit(Unit)
                delay(timerUpdateIntervalMillis)
            }
        }.collect {
            val now = LocalTime.now()

            val totalQuestSeconds = ChronoUnit.SECONDS.between(QUEST_START_TIME, QUEST_END_TIME).toFloat()
            val elapsedSeconds = ChronoUnit.SECONDS.between(QUEST_START_TIME, now).toFloat()

            progress = when {
                now.isBefore(QUEST_START_TIME) -> 0f
                now.isAfter(QUEST_END_TIME) -> 1f
                else -> (elapsedSeconds / totalQuestSeconds).coerceIn(0f, 1f)
            }
        }
    }

    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500)
    ).value

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = progressBarTrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                brush = progressBarBrush,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}