package com.gcancino.levelingup.domain.models

import androidx.glance.text.FontWeight
import java.util.Date

data class BodyComposition(
    val uid: String,
    val weight: Double? = null,
    val bmi: Double? = null,
    val bodyFat: Double? = null,
    val muscleMass: Double? = null,
    val bodyAge: Int? = null,
    val visceralFat: Int? = null,
    val date: Date? = null,
    val photos: List<String>? = null,
    val isInitial: Boolean? = null
)
