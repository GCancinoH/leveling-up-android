package com.gcancino.levelingup.domain.models.exercise

import java.util.Date

data class Mesocycle(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val macrocycleId: String = "",
    val durationWeeks: Int = 0,
    val objective: MesocycleObjective = MesocycleObjective.HYPERTROPHY,
    val startDate: Date = Date(),
    val endDate: Date = Date(),
    val frequency: Int = 0,
    val daysOfWeek: List<WeekDay> = emptyList(),
    val microcycles: List<Microcycle> = emptyList()
)