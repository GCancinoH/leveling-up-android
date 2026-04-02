package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.EveningEntryEntity
import com.gcancino.levelingup.domain.models.dailyTasks.EveningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.google.gson.Gson

private val gson = Gson()

fun EveningEntryEntity.toDomain() = EveningEntry(
    id = id,
    uID = uID,
    date = date,
    answers = gson.fromJson(answers, Array<ReflectionAnswer>::class.java).toList(),
    isSynced = isSynced
)

fun EveningEntry.toEntity() = EveningEntryEntity(
    id = id,
    uID = uID,
    date = date,
    answers = gson.toJson(answers),
    isSynced = isSynced
)