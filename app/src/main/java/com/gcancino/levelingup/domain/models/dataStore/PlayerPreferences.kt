package com.gcancino.levelingup.domain.models.dataStore

import java.time.LocalDate

data class PlayerPreferences(
    val isPlayerDataSavedLocally: Boolean,
    val areQuestsLoaded: Boolean,
    val lastQuestReset: LocalDate,
    val lastPenaltyXpLost: Int,
    val lastPenaltyStreakLost: Int,
    val lastPenaltyDate: LocalDate,
    val lastPenaltyTasksCount: Int
)
