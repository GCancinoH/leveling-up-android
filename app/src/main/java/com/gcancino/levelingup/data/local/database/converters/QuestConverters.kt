package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestStatus
import com.gcancino.levelingup.domain.models.QuestStreak
import com.gcancino.levelingup.domain.models.player.Improvement
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.Date

class QuestConverters {
    private val gson = Gson()
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
        return gson.toJson(value)
    }

    @TypeConverter
    fun toQuestRewards(value: String?): QuestRewards? {
        return value?.let { gson.fromJson(it, QuestRewards::class.java) }
    }

    @TypeConverter
    fun toQuestDetails(value: String?): QuestDetails? {
        return value?.let {
            gson.fromJson(it, QuestDetails::class.java)
        }
    }

    @TypeConverter
    fun fromQuestDetails(questDetails: QuestDetails?): String? {
        return questDetails?.let {
            gson.toJson(it)
        }
    }

    @TypeConverter
    fun fromQuestStreak(value: QuestStreak?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toQuestStreak(value: String?): QuestStreak? {
        return value?.let { gson.fromJson(it, QuestStreak::class.java) }
    }
}