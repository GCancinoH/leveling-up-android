package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.identity.ObjectiveDao
import com.gcancino.levelingup.data.local.datastore.DataStoreCryptoManager
import com.gcancino.levelingup.data.mappers.ObjectiveMapper
import com.gcancino.levelingup.domain.models.identity.Objective
import com.gcancino.levelingup.domain.models.identity.TimeHorizon
import com.gcancino.levelingup.domain.repositories.ObjectiveRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectiveRepositoryImpl @Inject constructor(
    private val objectiveDao: ObjectiveDao,
    private val firestore: FirebaseFirestore,
    cryptoManager: DataStoreCryptoManager
) : ObjectiveRepository {

    private val TAG = "ObjectiveRepository"
    private val mapper = ObjectiveMapper(cryptoManager)

    override suspend fun saveObjective(objective: Objective): Resource<Unit> {
        return try {
            objectiveDao.insert(mapper.toEntity(objective))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveObjective failed")
            Resource.Error("Failed to save objective")
        }
    }

    override suspend fun updateObjectiveProgress(id: String, newValue: Float): Resource<Unit> {
        return try {
            objectiveDao.updateProgress(id, newValue)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update progress")
        }
    }

    override fun observeObjectives(uID: String): Flow<List<Objective>> {
        return objectiveDao.observeAll(uID).map { list ->
            list.map { mapper.toDomain(it) }
        }
    }

    override fun observeObjectivesByHorizon(uID: String, horizon: TimeHorizon): Flow<List<Objective>> {
        return objectiveDao.observeByHorizon(uID, horizon.name).map { list ->
            list.map { mapper.toDomain(it) }
        }
    }

    override fun observeObjectivesByRole(roleId: String): Flow<List<Objective>> {
        return objectiveDao.observeByRole(roleId).map { list ->
            list.map { mapper.toDomain(it) }
        }
    }

    override suspend fun getChildren(parentId: String): List<Objective> {
        return objectiveDao.getChildren(parentId).map { mapper.toDomain(it) }
    }

    override suspend fun deleteObjective(objective: Objective): Resource<Unit> {
        return try {
            objectiveDao.delete(mapper.toEntity(objective))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete objective")
        }
    }

    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val unsynced = objectiveDao.getUnsynced()
            if (unsynced.isEmpty()) return Resource.Success(Unit)

            val batch = firestore.batch()
            unsynced.forEach { entity ->
                val ref = firestore.collection("objectives").document(entity.id)
                // Note: We sync the encrypted description to Firestore (Zero Knowledge)
                batch.set(ref, entity)
            }

            batch.commit().await()
            objectiveDao.markAsSynced(unsynced.map { it.id })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced failed")
            Resource.Error("Sync failed")
        }
    }
}
