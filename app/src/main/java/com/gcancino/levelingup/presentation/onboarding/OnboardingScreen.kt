package com.gcancino.levelingup.presentation.onboarding

import android.app.Activity
import android.view.WindowInsetsController
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.onBoarding.OnboardingStep
import com.gcancino.levelingup.ui.theme.SystemColors

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onCompleted: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val data by viewModel.data.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label        = "onboardingProgress"
    )

    // Hide status bar for onboarding, restore on exit
    val view = LocalView.current
    val context = LocalContext.current

    // Navigate to dashboard after successful save
    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) onCompleted()
    }

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose { controller.show(WindowInsetsCompat.Type.statusBars()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemColors.BackgroundColor)
            .padding(start = 16.dp, top = 70.dp, end = 16.dp, bottom = 16.dp)
    ) {
        // ── Progress bar (hidden on welcome step) ─────────────────────────────────
        if (currentStep != OnboardingStep.WELCOME) {
            LinearProgressIndicator(
                progress     = { animatedProgress },
                modifier     = Modifier.fillMaxWidth(),
                color        = Color(0xFF4A69BD),
                trackColor   = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Step content ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState  = currentStep,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                },
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> {
                        OnboardingWelcomeStep(
                            onNext = { viewModel.nextStep() }
                        )
                    }
                    OnboardingStep.PERSONAL_INFO -> {
                        OnboardingPersonalInfoStep(
                            initialName     = data.displayName,
                            initialBirthDate = data.birthDate,
                            initialGender   = data.gender,
                            initialAge      = data.age,
                            onNext          = { name, birthDate, gender ->
                                viewModel.updatePersonalInfo(name, birthDate, gender)
                                viewModel.nextStep()
                            },
                            onBack = { viewModel.previousStep() },
                            onDismiss = { onDismiss() }
                        )
                    }
                    OnboardingStep.PHYSICAL_AND_COMPOSITION -> {
                        OnboardingPhysicalCompositionStep(
                            initialData = data,
                            onNext      = { height, weight, bmi, bodyFat, muscleMass, visceralFat, bodyAge, unit ->
                                viewModel.updatePhysicalAndComposition(
                                    height, weight, bmi, bodyFat, muscleMass, visceralFat, bodyAge, unit
                                )
                                viewModel.nextStep()
                            },
                            onBack = { viewModel.previousStep() }
                        )
                    }
                    OnboardingStep.BODY_MEASUREMENT -> {
                        OnboardingBodyMeasurementsStep(
                            initialData = data,
                            onNext      = { measurements ->
                                viewModel.updateMeasurements(
                                    measurements.neck, measurements.shoulders,
                                    measurements.chest, measurements.waist,
                                    measurements.umbilical, measurements.hip,
                                    measurements.bicepLeftRelaxed, measurements.bicepLeftFlexed,
                                    measurements.bicepRightRelaxed, measurements.bicepRightFlexed,
                                    measurements.forearmLeft, measurements.forearmRight,
                                    measurements.thighLeft, measurements.thighRight,
                                    measurements.calfLeft, measurements.calfRight
                                )
                                viewModel.nextStep()
                            },
                            onBack = { viewModel.previousStep() }
                        )
                    }
                    OnboardingStep.IMPROVEMENTS -> {
                        OnboardingImprovementsStep(
                            initialSelections = data.improvements,
                            onNext            = { improvements ->
                                viewModel.updateImprovements(improvements)
                                viewModel.nextStep()
                            },
                            onBack = { viewModel.previousStep() }
                        )
                    }
                    OnboardingStep.PHOTOS -> {
                        OnboardingPhotosStep(
                            selectedUris = data.photoUris,
                            saveState    = saveState,
                            onAddPhoto   = { viewModel.addPhoto(it) },
                            onRemovePhoto = { viewModel.removePhoto(it) },
                            onFinish     = { viewModel.saveAll(onCompleted) },
                            onBack       = { viewModel.previousStep() }
                        )
                    }
                }
            }
        }
    }
}