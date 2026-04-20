package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.WeeklyEntryDao
import com.gcancino.levelingup.data.local.datastore.DataStoreCryptoManager
import com.gcancino.levelingup.data.mappers.WeeklyEntryMapper
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyEntry
import com.gcancino.levelingup.domain.repositories.ReflectionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReflectionRepositoryImpl @Inject constructor(
    private val weeklyEntryDao: WeeklyEntryDao,
    private val firestore: FirebaseFirestore,
    cryptoManager: DataStoreCryptoManager
) : ReflectionRepository {

    private val TAG = "ReflectionRepository"
    private val weeklyMapper = WeeklyEntryMapper(cryptoManager)

    override suspend fun saveWeeklyEntry(entry: WeeklyEntry): Resource<Unit> {
        return try {
            weeklyEntryDao.insert(weeklyMapper.toEntity(entry))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveWeeklyEntry failed")
            Resource.Error("Failed to save weekly entry")
        }
    }

    override fun observeWeeklyEntry(uID: String, year: Int, weekNumber: Int): Flow<WeeklyEntry?> {
        return weeklyEntryDao.observeByWeek(uID, year, weekNumber).map { entity ->
            entity?.let { weeklyMapper.toDomain(it) }
        }
    }

    override fun observeAllWeeklyEntries(uID: String): Flow<List<WeeklyEntry>> {
        return weeklyEntryDao.observeAll(uID).map { list ->
            list.map { weeklyMapper.toDomain(it) }
        }
    }

    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val unsynced = weeklyEntryDao.getUnsynced()
            if (unsynced.isEmpty()) return Resource.Success(Unit)

            val batch = firestore.batch()
            unsynced.forEach { entity ->
                val ref = firestore.collection("weekly_entries").document(entity.id)
                batch.set(ref, entity)
            }

            batch.commit().await()
            weeklyEntryDao.markAsSynced(unsynced.map { it.id })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced failed")
            Resource.Error("Sync failed")
        }
    }
}
