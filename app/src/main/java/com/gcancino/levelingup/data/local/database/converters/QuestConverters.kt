package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestStatus
import com.gcancino.levelingup.domain.models.QuestStreak
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Date

class QuestConverters {
    private val json = Json { ignoreUnknownKeys = true }
    // firestore key: AVW2 SKTR 3W33 KN4T W7P1 14B2 PCHV Z0VS

    @TypeConverter
    fun fromQuestStatus(value: QuestStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toQuestStatus(value: String?): QuestStatus? {
        return value?.let { QuestStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromQuestRewards(value: QuestRewards?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toQuestRewards(value: String?): QuestRewards? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun toQuestDetails(value: String?): QuestDetails? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromQuestDetails(questDetails: QuestDetails?): String? {
        return questDetails?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun fromQuestStreak(value: QuestStreak?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toQuestStreak(value: String?): QuestStreak? {
        return value?.let { json.decodeFromString(it) }
    }
}