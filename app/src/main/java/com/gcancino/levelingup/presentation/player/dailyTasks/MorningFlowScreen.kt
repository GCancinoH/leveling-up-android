package com.gcancino.levelingup.presentation.player.dailyTasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.presentation.player.dailyTasks.shared.ReflectionQuestionCard
import com.gcancino.levelingup.presentation.player.dailyTasks.shared.resolveTexts
import com.gcancino.levelingup.presentation.player.dailyTasks.viewModels.MorningFlowViewModel
import com.gcancino.levelingup.ui.components.notifications.SystemNotificationOverlay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun MorningFlowScreen(
    viewModel: MorningFlowViewModel = hiltViewModel(),
    onCompleted: () -> Unit
) {
    val currentStep by viewModel.currentStepIndex.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val isCurrentAnswerValid by viewModel.isCurrentAnswerValid.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val questions = viewModel.questions
    val penalty by viewModel.lastNightPenalty.collectAsState()
    val isLastStep by viewModel.isLastStep.collectAsState()

    //val questionTexts = questions.associate { it.id to stringResource(it.textRes) }
    val questionTexts = questions.resolveTexts()

    // Fixed: Use penalty != null as a key for remember
    var showPenaltySummary by remember(penalty != null) { mutableStateOf(penalty != null) }
    var showSystemNotification by remember { mutableStateOf(false) }

    
    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) showSystemNotification = true
    }

    val currentPenalty = penalty
    if (showPenaltySummary && currentPenalty != null) {
        PenaltySummaryScreen(
            penalty = currentPenalty,
            onDismiss = { showPenaltySummary = false }
        )
        return
    }

    val animatedProgress by animateFloatAsState(
        targetValue   = (currentStep + 1).toFloat() / viewModel.totalSteps.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label         = "morningProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Good Morning 🌅",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White
                )
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                "${currentStep + 1} / ${viewModel.totalSteps}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress     = { animatedProgress },
            modifier     = Modifier.fillMaxWidth(),
            color        = Color(0xFFFFB300),
            trackColor   = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Question
        AnimatedContent(
            targetState  = currentStep,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label        = "morningQuestion"
        ) { step ->
            val question = questions[step]
            ReflectionQuestionCard(
                question = question,
                answer   = answers[question.id] ?: "",
                onAnswerChange = { viewModel.updateAnswer(question.id, it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                TextButton(onClick = { viewModel.previousStep() }) {
                    Text("Back", color = Color.Gray)
                }
            } else Spacer(modifier = Modifier.width(1.dp))

            Button(
                onClick  = {
                    if (isLastStep) viewModel.save(questionTexts)
                    else viewModel.nextStep()
                },
                enabled = isCurrentAnswerValid && saveState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFFFB300),
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (saveState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isLastStep) "Finish" else "Next",
                        color      = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SystemNotificationOverlay(
            message = "MORNING REFLECTION COMPLETE",
            subMessage = "REWARD: +10 XP RECEIVED",
            isVisible = showSystemNotification,
            onTimeout = {
                showSystemNotification = false
                onCompleted()
            }
        )
    }
}
