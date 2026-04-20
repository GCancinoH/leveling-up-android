package com.gcancino.levelingup.data.mappers

import com.gcancino.levelingup.data.local.database.entities.QuestEntity
import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestStatus
import com.gcancino.levelingup.domain.models.QuestStreak
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Date

private val json = Json { ignoreUnknownKeys = true }

fun Quests.toEntity(): QuestEntity {
    return QuestEntity(
        id = id,
        types = types?.let { json.encodeToString(it) },
        title = title,
        description = description,
        status = status?.name,
        date = date.time,
        startedDate = startedDate?.time,
        finishedDate = finishedDate?.time,
        rewards = rewards?.let { json.encodeToString(it) },
        details = details?.let { json.encodeToString(it) },
        streak = streak?.let { json.encodeToString(it) }
    )
}

fun QuestEntity.toDomain(): Quests {
    return Quests(
        id = id,
        types = types?.let { json.decodeFromString<List<Improvement>>(it) },
        title = title,
        description = description,
        status = status?.let { QuestStatus.valueOf(it) },
        date = Date(date),
        startedDate = startedDate?.let { Date(it) },
        finishedDate = finishedDate?.let { Date(it) },
        rewards = rewards?.let { json.decodeFromString<QuestRewards>(it) },
        details = details?.let { json.decodeFromString<QuestDetails>(it) },
        streak = streak?.let { json.decodeFromString<QuestStreak>(it) }
    )
}
