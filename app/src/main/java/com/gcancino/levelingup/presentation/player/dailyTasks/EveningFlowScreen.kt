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
import com.gcancino.levelingup.presentation.player.dailyTasks.shared.TaskCreationStep
import com.gcancino.levelingup.presentation.player.dailyTasks.shared.resolveTexts
import com.gcancino.levelingup.presentation.player.dailyTasks.viewModels.EveningFlowViewModel
import com.gcancino.levelingup.ui.components.notifications.SystemNotificationOverlay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun EveningFlowScreen(
    viewModel: EveningFlowViewModel = hiltViewModel(),
    onCompleted: () -> Unit
) {
    val currentStep by viewModel.currentStepIndex.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val questions = viewModel.questions
    val isCurrentAnswerValid by viewModel.isCurrentAnswerValid.collectAsState()
    val isLastStep by viewModel.isLastStep.collectAsState()
    val canAddMoreTasks by viewModel.canAddMoreTasks.collectAsState()
    var showSystemMessage by remember { mutableStateOf(false) }

    //val questionTexts = questions.associate { it.id to stringResource(it.textRes) }
    val questionTexts = questions.resolveTexts()

    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) showSystemMessage = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue   = (currentStep + 1).toFloat() / viewModel.totalSteps.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label         = "eveningProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(24.dp)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Evening Reflection 🌙",
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
            progress   = { animatedProgress },
            modifier   = Modifier.fillMaxWidth(),
            color      = Color(0xFF7986CB),
            trackColor = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(
            targetState  = currentStep,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label        = "eveningStep"
        ) { step ->
            if (viewModel.currentPhase == EveningFlowViewModel.EveningStep.QUESTIONS) {
                val question = questions[step]
                ReflectionQuestionCard(
                    question     = question,
                    answer       = answers[question.id] ?: "",
                    onAnswerChange = { viewModel.updateAnswer(question.id, it) },
                    accentColor  = Color(0xFF7986CB)
                )
            } else {
                TaskCreationStep(
                    tasks          = tasks,
                    canAddMore     = canAddMoreTasks,
                    onAddTask      = { title, priority -> viewModel.addTask(title, priority) },
                    onRemoveTask   = { viewModel.removeTask(it) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

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
                enabled  = isCurrentAnswerValid && saveState !is Resource.Loading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF7986CB),
                    disabledContainerColor = Color.DarkGray
                ),
                shape    = RoundedCornerShape(16.dp)
            ) {
                if (saveState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isLastStep) "Save & Finish" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SystemNotificationOverlay(
            message = "EVENING REFLECTION COMPLETE",
            subMessage = "REWARD +15 XP RECEIVED",
            isVisible = showSystemMessage,
            onTimeout = {
                showSystemMessage = false
                onCompleted()
            }
        )
    }
}