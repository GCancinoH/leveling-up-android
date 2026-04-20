package com.gcancino.levelingup.data.mappers

import com.gcancino.levelingup.data.local.database.entities.identity.ObjectiveEntity
import com.gcancino.levelingup.data.local.datastore.DataStoreCryptoManager
import com.gcancino.levelingup.domain.models.identity.Objective
import com.gcancino.levelingup.domain.models.identity.ObjectiveStatus
import com.gcancino.levelingup.domain.models.identity.TimeHorizon

class ObjectiveMapper(private val cryptoManager: DataStoreCryptoManager) {

    fun toEntity(domain: Objective): ObjectiveEntity {
        val encryptedDescription = cryptoManager.encrypt(domain.description) ?: domain.description
        return ObjectiveEntity(
            id = domain.id,
            uID = domain.uID,
            roleId = domain.roleId,
            parentId = domain.parentId,
            title = domain.title,
            description = encryptedDescription,
            horizon = domain.horizon.name,
            status = domain.status.name,
            targetValue = domain.targetValue,
            currentValue = domain.currentValue,
            unit = domain.unit,
            startDate = domain.startDate,
            endDate = domain.endDate,
            createdAt = domain.createdAt,
            completedAt = domain.completedAt,
            isSynced = domain.isSynced
        )
    }

    fun toDomain(entity: ObjectiveEntity): Objective {
        val decryptedDescription = cryptoManager.decrypt(entity.description) ?: entity.description
        return Objective(
            id = entity.id,
            uID = entity.uID,
            roleId = entity.roleId,
            parentId = entity.parentId,
            title = entity.title,
            description = decryptedDescription,
            horizon = TimeHorizon.valueOf(entity.horizon),
            status = ObjectiveStatus.valueOf(entity.status),
            targetValue = entity.targetValue,
            currentValue = entity.currentValue,
            unit = entity.unit,
            startDate = entity.startDate,
            endDate = entity.endDate,
            createdAt = entity.createdAt,
            completedAt = entity.completedAt,
            isSynced = entity.isSynced
        )
    }
}
