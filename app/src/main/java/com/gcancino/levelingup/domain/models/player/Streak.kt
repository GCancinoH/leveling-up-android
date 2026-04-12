package com.gcancino.levelingup.domain.models.player

import com.gcancino.levelingup.core.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Streak(
    val uid: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStreakUpdate: @Serializable (with = DateSerializer::class) Date? = null,
    val protectedDays: Int = 0 // tokens: 1 token = 1 día fallado perdonado
)