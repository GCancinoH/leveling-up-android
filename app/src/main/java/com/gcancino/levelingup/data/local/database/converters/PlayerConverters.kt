package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PlayerConverters {
    private val json = Json { ignoreUnknownKeys = true }

    // Improvements Converter
    @TypeConverter
    fun fromImprovements(improvements: List<Improvement>?): String? = improvements?.let { json.encodeToString(it) }

    @TypeConverter
    fun toImprovements(jsonStr: String?): List<Improvement>? {
        return jsonStr?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromGender(gender: Genders?): String? = gender?.name

    @TypeConverter
    fun toGender(gender: String?): Genders? = gender?.let {
        try {
            Genders.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
}