package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.MorningEntryEntity
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun MorningEntryEntity.toDomain() = MorningEntry(
    id = id,
    uID = uID,
    date = date,
    answers = json.decodeFromString(answers),
    isSynced = isSynced
)

fun MorningEntry.toEntity() = MorningEntryEntity(
    id = id,
    uID = uID,
    date = date,
    answers = json.encodeToString(answers),
    isSynced = isSynced
)