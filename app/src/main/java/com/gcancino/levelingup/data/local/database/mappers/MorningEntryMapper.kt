package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.MorningEntryEntity
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.google.gson.Gson

private val gson = Gson()
fun MorningEntryEntity.toDomain() = MorningEntry(
    id = id,
    uID = uID,
    date = date,
    answers = gson.fromJson(answers, Array<ReflectionAnswer>::class.java).toList(),
    isSynced = isSynced
)

fun MorningEntry.toEntity() = MorningEntryEntity(
    id = id,
    uID = uID,
    date = date,
    answers = gson.toJson(answers),
    isSynced = isSynced
)