package com.gcancino.levelingup.domain.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.identity.Objective
import com.gcancino.levelingup.domain.models.identity.TimeHorizon
import kotlinx.coroutines.flow.Flow

interface ObjectiveRepository {
    suspend fun saveObjective(objective: Objective): Resource<Unit>
    suspend fun updateObjectiveProgress(id: String, newValue: Float): Resource<Unit>
    fun observeObjectives(uID: String): Flow<List<Objective>>
    fun observeObjectivesByHorizon(uID: String, horizon: TimeHorizon): Flow<List<Objective>>
    fun observeObjectivesByRole(roleId: String): Flow<List<Objective>>
    suspend fun getChildren(parentId: String): List<Objective>
    suspend fun deleteObjective(objective: Objective): Resource<Unit>
    suspend fun syncUnsynced(): Resource<Unit>
}
