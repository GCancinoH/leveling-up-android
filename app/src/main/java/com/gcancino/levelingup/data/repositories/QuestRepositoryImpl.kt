package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.database.dao.QuestDao
import com.gcancino.levelingup.data.mappers.toDomain
import com.gcancino.levelingup.data.mappers.toEntity
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.repositories.QuestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.collections.emptyList
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class QuestRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val questDao: QuestDao,
    private val playerDao: PlayerDao
) : QuestRepository {

    private val auth = FirebaseAuth.getInstance()

    override suspend fun syncQuestsFromFirestoreDos(): Resource<Unit> {
        return try {
            val questsFromFirestore = getQuestsFromFirestore()
            Timber.tag("QuestRepository").d("Quests from Firestore: $questsFromFirestore")
            val questEntities = questsFromFirestore.map { it.toEntity() }

            questDao.deleteAllQuests()
            val insertionResults: List<Long> = questDao.insertQuests(questEntities)

            // Check 1: Did insertQuests report successful insertions?
            // A rowId of -1 usually indicates an error or conflict that prevented insertion.
            val successfulInserts = insertionResults.count { it > 0L } // Count valid rowIds

            if (questEntities.isNotEmpty() && successfulInserts == questEntities.size) {
                // All entities that we attempted to insert seem to have been inserted successfully by Room.
                Timber.tag("QuestRepository")
                    .d("Successfully inserted $successfulInserts quests into Room.")
                Resource.Success(Unit) // Or you could return Resource.Success(successfulInserts)
            } else if (questEntities.isEmpty() && successfulInserts == 0) {
                // No quests from Firestore, nothing to insert, which is a valid success state.
                Timber.tag("QuestRepository").d("No quests from Firestore to insert.")
                Resource.Success(Unit)
            } else {
                // Partial success or failure in insertion according to Room.
                Timber.tag("QuestRepository")
                    .w("Insertion issue: Attempted to insert ${questEntities.size}, Room reported $successfulInserts successful rowIds. Results: $insertionResults")
                Resource.Error("Failed to insert all quests. Successful: $successfulInserts/${questEntities.size}")
            }
        } catch (e: Exception) {
            Timber.tag("QuestRepository")
                .e(e, "Exception during Firestore sync or DB operation: ${e.message}")
            Resource.Error(e.message ?: "Unknown error occurred during sync")
        }
        /*return try {
            val questsFromFirestore = getQuestsFromFirestore()

            questDao.deleteAllQuests()
            val result = questDao.insertQuests(
                questsFromFirestore.map { it.toEntity() }
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }*/
    }

    override suspend fun syncQuestsFromFirestore(): Resource<Unit> {
        val player = auth.currentUser ?: return Resource.Error("Player not found")

        return try {
            val playerImprovements = playerDao.getPlayerImprovements(player.uid)
            val questsFromFirestore = getQuestsFromFirestore()

            val filteredQuests = questsFromFirestore.filter { quest ->
                quest.types?.any { questType ->
                    playerImprovements.contains(questType)
                } ?: false
            }

            questDao.deleteAllQuests()
            questDao.insertQuests(
                filteredQuests.map { it.toEntity() }
            )

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    override fun getNotStartedQuests() : Flow<List<Quests>> = flow {
        try {
            questDao.getNotStartedQuests().collect { quests ->
                emit(quests.map { it.toDomain() })
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getQuestByQuestID(questID: String): Resource<Quests> {
        return try {
            val questEntity = questDao.getQuestById(questID)
            if (questEntity != null) {
                Resource.Success(questEntity.toDomain())
            } else {
                Resource.Error("Quest not found")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to get quest: ${e.message}", exception = e)
        }
    }
        /*return flow {
            emit(Resource.Loading())


        }.catch { e ->
            emit(Resource.Error("Error fetching quest: ${e.localizedMessage ?: "Unknown error"}"))
        }.flowOn(Dispatchers.IO)*/

    private suspend fun getQuestsFromFirestore(): List<Quests> {
        return try {
            val querySnapshot = db.collection("all_quests")
                .get()
                .await()

            val questsList = querySnapshot.documents.mapNotNull { documentSnapshot ->
                try {
                    documentSnapshot.toObject<Quests>()
                } catch (e: Exception) {
                    Timber.tag("Firestore")
                        .e(e, "Error converting document ${documentSnapshot.id} to Quests")
                    null
                }
            }
            Timber.tag("Firestore").d("Successfully fetched ${questsList.size} quests.")
            questsList
        } catch (e: CancellationException) {
            Timber.tag("Firestore").d("Firestore getQuests operation was cancelled.")
            throw e
        } catch (e: Exception) {
            Timber.tag("Firestore").e(e, "Error fetching quests from Firestore: ${e.message}")
            emptyList()
        }
    }

    override suspend fun updateQuestStatus(questID: String) : Resource<Unit> {
        return try {
            questDao.updateQuestStatusToInProgress(questID)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}