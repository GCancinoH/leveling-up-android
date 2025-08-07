package com.gcancino.levelingup.domain.models.player

import java.util.Date

data class Streak(
    val uid: String,
    val currentStreak: Int? = null,
    val longestStreak: Int? = null,
    val lastStreakUpdate: Date? = null
)
