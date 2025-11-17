package com.gcancino.levelingup.presentation.auth.signUp

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gcancino.levelingup.presentation.auth.signUp.steps.AccountStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.BodyCompositionStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.ImprovementStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.ObjectivesStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.PersonalInfoStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.PhotosStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.PhysicalAttributesStep
import com.gcancino.levelingup.presentation.auth.signUp.steps.WelcomeStep
import com.gcancino.levelingup.ui.theme.BackgroundColor

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel
) {

    val animatedProgress by animateFloatAsState(
        targetValue = viewModel.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = ""
    )
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp)
            .background(BackgroundColor)
    ) {
        if(viewModel.currentStep != 0) {
            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4A69BD),
                    trackColor = Color.White,

                    )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = viewModel.currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500)) using
                            SizeTransform { initialSize, targetSize ->
                                keyframes {
                                    durationMillis = 500
                                    initialSize at 0 using LinearOutSlowInEasing
                                    targetSize at 300 using FastOutSlowInEasing
                                }
                            }
                }, label = ""
            ) { targetStep: Int ->
                when (targetStep) {
                    0 -> WelcomeStep(viewModel)
                    1 -> AccountStep(
                        viewModel = viewModel.accountViewModel,
                        signUpViewModel = viewModel
                    )
                    2 -> PersonalInfoStep(
                        viewModel = viewModel.personalInfoViewModel,
                        signUpViewModel = viewModel
                    )
                    3 -> PhysicalAttributesStep(viewModel.physicalAttributesViewModel)
                    4 -> BodyCompositionStep(viewModel.bodyCompositionViewModel)
                    5 -> ImprovementStep(viewModel.improvementsViewModel)
                    6 -> PhotosStep(viewModel.photosViewModel)
                }
            }
        }
    }
}