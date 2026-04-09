package com.gcancino.levelingup.domain.models.identity

import java.util.Date

data class GeneratedQuest(
    val id: String,
    val uID: String,
    val weeklyReportId: String,         // which report spawned this quest
    val title: String,
    val description: String,
    val type: GeneratedQuestType,
    val targetStandardIds: List<String>, // IDs of the failing standards
    val goal: Int,                       // e.g. 5 days in a row, 4 times in a week
    val durationDays: Int,               // how long the quest lasts
    val currentProgress: Int = 0,        // updated daily by DailyResetManager
    val status: GeneratedQuestStatus = GeneratedQuestStatus.ACTIVE,
    val startDate: Date = Date(),
    val endDate: Date,                   // startDate + durationDays
    val completedAt: Date? = null,
    val xpReward: Int = 150,            // completing a corrective quest = big reward
    val isSynced: Boolean = false
)

enum class GeneratedQuestType {
    STREAK,       // complete a standard X days in a row
    CONSISTENCY,  // complete a standard N times in a week
    ELIMINATION   // avoid failing a standard (zero failures in durationDays)
}

enum class GeneratedQuestStatus {
    ACTIVE,
    COMPLETED,
    FAILED,      // user missed the window
    EXPIRED      // time ran out without completion
}