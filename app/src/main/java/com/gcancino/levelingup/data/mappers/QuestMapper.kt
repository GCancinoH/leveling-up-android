package com.gcancino.levelingup.data.mappers

import com.gcancino.levelingup.data.local.database.entities.QuestEntity
import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestStatus
import com.gcancino.levelingup.domain.models.QuestStreak
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.models.player.Improvement
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.util.Date

fun Quests.toEntity(): QuestEntity {
    return QuestEntity(
        id = id,
        types = types?.let { Gson().toJson(it) },
        title = title,
        description = description,
        status = status?.name,
        date = date.time,
        startedDate = startedDate?.time,
        finishedDate = finishedDate?.time,
        rewards = rewards?.let { Gson().toJson(it) },
        details = details?.let { Gson().toJson(it) },
        streak = streak?.let { Gson().toJson(it) }
    )
}

fun QuestEntity.toDomain(): Quests {
    val gson = Gson()
    return Quests(
        id = id,
        types = types?.let {
            val listType = object : TypeToken<List<Improvement>>() {}.type
            gson.fromJson<List<Improvement>>(it, listType)
        },
        title = title,
        description = description,
        status = status?.let { QuestStatus.valueOf(it) },
        date = Date(date),
        startedDate = startedDate?.let { Date(it) },
        finishedDate = finishedDate?.let { Date(it) },
        rewards = rewards?.let { gson.fromJson(it, QuestRewards::class.java) },
        details = details?.let { gson.fromJson(it, QuestDetails::class.java) },
        streak = streak?.let { gson.fromJson(it, QuestStreak::class.java) }
    )
}
