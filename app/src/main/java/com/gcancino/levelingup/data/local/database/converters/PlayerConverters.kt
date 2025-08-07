package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.player.Improvement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class PlayerConverters {
    private val gson = Gson()

    // Date Converters
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }

    // Improvements Converter
    @TypeConverter
    fun fromImprovements(improvements: List<Improvement>?): String? = gson.toJson(improvements)

    @TypeConverter
    fun toImprovements(json: String?): List<Improvement>? {
        val list = object : TypeToken<List<Improvement>>() {}.type
        return gson.fromJson(json, list)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        }
    }

}