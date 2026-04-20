package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Date

class CommonConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(name: String): TaskPriority = try {
        TaskPriority.valueOf(name)
    } catch (e: Exception) {
        TaskPriority.LOW
    }
}
