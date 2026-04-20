package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.EveningEntryEntity
import com.gcancino.levelingup.domain.models.dailyTasks.EveningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun EveningEntryEntity.toDomain() = EveningEntry(
    id = id,
    uID = uID,
    date = date,
    answers = json.decodeFromString(answers),
    isSynced = isSynced
)

fun EveningEntry.toEntity() = EveningEntryEntity(
    id = id,
    uID = uID,
    date = date,
    answers = json.encodeToString(answers),
    isSynced = isSynced
)