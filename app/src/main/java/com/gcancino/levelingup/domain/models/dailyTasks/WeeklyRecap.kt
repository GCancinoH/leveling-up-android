package com.gcancino.levelingup.domain.models.dailyTasks

import com.gcancino.levelingup.domain.models.identity.Objective

data class WeeklyRecap(
    val weekNumber: Int,
    val year: Int,
    val dailyWins: List<String>,
    val challengesFaced: List<String>,
    val completedObjectives: List<Objective>,
    val pendingObjectives: List<Objective>,
    val totalXpGained: Int,
    val identityAlignmentScore: Float
)
