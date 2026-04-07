package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable
import java.util.Date
import com.gcancino.levelingup.core.DateSerializer

@Serializable
data class DailyStandardEntry(
    val id: String = "",
    val uID: String = "",
    val standardId: String = "",
    val standardTitle: String = "",
    val standardType: StandardType = StandardType.CUSTOM,
    val roleId: String = "",
    val roleName: String  = "",
    val date: @Serializable(with = DateSerializer::class) Date = Date(),
    val isCompleted: Boolean = false,
    val completedAt: @Serializable(with = DateSerializer::class) Date? = null,
    val xpAwarded: Int = 0,
    val autoValidated: Boolean = false,
    val penaltyApplied: Boolean = false,
    val isSynced: Boolean = false
)
