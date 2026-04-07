package com.gcancino.levelingup.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Identity Profile
 */
@Entity(tableName = "identity_profile")
data class IdentityProfileEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val identityStatement: String,
    val roles: String,
    val standards: String,
    val createdAt: Date,
    val isSynced: Boolean = false
)

/**
 * Daily Standard Entry
 */
@Entity(tableName = "daily_standard_entries")
data class DailyStandardEntryEntity(
    @PrimaryKey val id: String,
    val uID: String,
    val standardId: String,
    val standardTitle: String,
    val standardType: String,
    val roleId: String,
    val roleName: String,
    val date: Date,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val xpAwarded: Int = 0,
    val autoValidated: Boolean = false,
    val penaltyApplied: Boolean = false,
    val isSynced: Boolean = false
)
