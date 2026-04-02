package com.gcancino.levelingup.domain.models.exercise

import java.util.Date

data class Macrocycle(
    val uID: String = "",
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val mesocycles: List<Mesocycle> = emptyList()
)