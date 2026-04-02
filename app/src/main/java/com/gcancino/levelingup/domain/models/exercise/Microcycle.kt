package com.gcancino.levelingup.domain.models.exercise

import java.util.Date

data class Microcycle(
    val id: String = "",
    val macrocycleId: String = "",
    val mesocycleId: String = "",
    val name: String = "",
    val week: Int = 0,
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val sessions: List<TrainingSession> = emptyList()
)
