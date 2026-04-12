package com.gcancino.levelingup.domain.models.onBoarding

import android.net.Uri
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import java.util.Date

data class OnboardingData(
    // Personal Info
    val displayName: String = "",
    val birthDate: Date? = null,
    val gender: Genders? = null,
    val age: Int = 0,

    // Physical + Composition (merged step)
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val bmi: Double = 0.0,
    val bodyFatPercentage: Double = 0.0,
    val muscleMassPercentage: Double = 0.0,
    val visceralFat: Double = 0.0,
    val bodyAge: Int = 0,
    val unitSystem: UnitSystem = UnitSystem.METRIC,

    // Body Measurements
    val neck: Double   = 0.0,
    val shoulders: Double   = 0.0,
    val chest: Double  = 0.0,
    val waist: Double  = 0.0,
    val umbilical: Double   = 0.0,
    val hip: Double = 0.0,
    val bicepLeftRelaxed: Double = 0.0,
    val bicepLeftFlexed: Double = 0.0,
    val bicepRightRelaxed: Double = 0.0,
    val bicepRightFlexed: Double = 0.0,
    val forearmLeft: Double = 0.0,
    val forearmRight: Double = 0.0,
    val thighLeft: Double = 0.0,
    val thighRight: Double = 0.0,
    val calfLeft: Double = 0.0,
    val calfRight: Double = 0.0,

    // Improvements
    val improvements: List<Improvement> = emptyList(),

    // Photos (URIs — uploaded to Storage at save time)
    val photoUris: List<Uri> = emptyList()
)
