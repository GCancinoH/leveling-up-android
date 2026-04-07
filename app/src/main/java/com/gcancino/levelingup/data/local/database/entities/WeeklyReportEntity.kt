package com.gcancino.levelingup.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val weekStart: Date,
    val overallScore: Float,
    val headline: String,
    val strongestRole: String,
    val weakestRole: String,
    val patternIdentified: String,
    val mirrorInsight: String,
    val oneCorrection: String,
    val identityAlignment: String,
    val generatedAt: Date,
    val isSynced: Boolean = false
)
