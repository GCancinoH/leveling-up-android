package com.gcancino.levelingup.domain.models.dataStore

import java.time.LocalDate

data class PlayerPreferences(
    val isPlayerDataSavedLocally: Boolean,
    val areQuestsLoaded: Boolean,
    val lastQuestReset: LocalDate
)
