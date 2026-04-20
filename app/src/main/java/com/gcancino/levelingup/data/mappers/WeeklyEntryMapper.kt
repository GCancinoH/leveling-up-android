package com.gcancino.levelingup.data.mappers

import com.gcancino.levelingup.data.local.database.entities.dailyTasks.WeeklyEntryEntity
import com.gcancino.levelingup.data.local.datastore.DataStoreCryptoManager
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class WeeklyEntryMapper(private val cryptoManager: DataStoreCryptoManager) {

    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(domain: WeeklyEntry): WeeklyEntryEntity {
        val answersJson = json.encodeToString(domain.answers)
        val winsJson = json.encodeToString(domain.winHighlights)
        
        val encryptedAnswers = cryptoManager.encrypt(answersJson) ?: answersJson
        val encryptedWins = cryptoManager.encrypt(winsJson) ?: winsJson
        
        return WeeklyEntryEntity(
            id = domain.id,
            uID = domain.uID,
            weekNumber = domain.weekNumber,
            year = domain.year,
            answers = encryptedAnswers,
            winHighlights = encryptedWins,
            alignmentScore = domain.alignmentScore,
            createdAt = domain.createdAt,
            isSynced = domain.isSynced
        )
    }

    fun toDomain(entity: WeeklyEntryEntity): WeeklyEntry {
        val decryptedAnswers = cryptoManager.decrypt(entity.answers) ?: entity.answers
        val decryptedWins = cryptoManager.decrypt(entity.winHighlights) ?: entity.winHighlights
        
        return WeeklyEntry(
            id = entity.id,
            uID = entity.uID,
            weekNumber = entity.weekNumber,
            year = entity.year,
            answers = try { json.decodeFromString(decryptedAnswers) } catch (e: Exception) { emptyList() },
            winHighlights = try { json.decodeFromString(decryptedWins) } catch (e: Exception) { emptyList() },
            alignmentScore = entity.alignmentScore,
            createdAt = entity.createdAt,
            isSynced = entity.isSynced
        )
    }
}
