package com.gcancino.levelingup.data.repositories

import androidx.room.withTransaction
import com.gcancino.levelingup.data.local.database.AppDatabase
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.mappers.toDomain
import com.gcancino.levelingup.data.mappers.toEntity
import com.gcancino.levelingup.domain.logic.LevelCalculator
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.Attributes
import com.gcancino.levelingup.domain.models.player.CategoryType
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.domain.models.player.Progress
import com.gcancino.levelingup.domain.models.player.Streak
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localDB: AppDatabase
) : PlayerRepository {

    override fun getPlayerData(): Flow<Resource<PlayerData>> = flow {
        val user = auth.currentUser ?: run {
            emit(Resource.Error("User not logged in"))
            return@flow
        }
        val uid = user.uid

        // Helper to fetch local data
        suspend fun fetchLocal(): PlayerData? = withContext(Dispatchers.IO) {
            val player = localDB.playerDao().getPlayer(uid)?.toDomain() ?: return@withContext null
            val attributes = localDB.playerAttributesDao().getPlayerAttributes(uid)?.toDomain()
            val progress = localDB.playerProgressDao().getPlayerProgress(uid)?.toDomain()
            val streak = localDB.playerStreakDao().getPlayerStreak(uid)?.toDomain()
            PlayerData(player, attributes, progress, streak)
        }

        // Try local first
        val localData = fetchLocal()
        if (localData != null) {
            emit(Resource.Success(localData))
            // Note: We do NOT call sync here; the ViewModel will handle it.
            return@flow
        }

        // No local data at all → fetch from Firestore
        emit(Resource.Loading())
        val remoteData = fetchFullPlayerDataFromFirestore(uid)

        // Create final PlayerData by filling missing fields with defaults
        val finalData = remoteData?.let {
            PlayerData(
                player = it.player,
                attributes = it.attributes ?: createDefaultAttributes(uid),
                progress = it.progress ?: createDefaultProgress(uid),
                streak = it.streak ?: createDefaultStreak(uid)
            )
        } ?: run {
            // If no remote data at all (e.g., new user without any Firestore docs), create all defaults
            PlayerData(
                player = Player(uid = uid),
                attributes = createDefaultAttributes(uid),
                progress = createDefaultProgress(uid),
                streak = createDefaultStreak(uid)
            )
        }

        // Save the final data to local DB
        withContext(Dispatchers.IO) {
            localDB.withTransaction {
                finalData.player?.let { localDB.playerDao().insertPLayer(it.toEntity()) }
                finalData.attributes?.let { localDB.playerAttributesDao().insertPlayerAttributes(it.toEntity()) }
                finalData.progress?.let { localDB.playerProgressDao().insertPlayerProgress(it.toEntity()) }
                finalData.streak?.let { localDB.playerStreakDao().insertPlayerStreak(it.toEntity()) }
            }
        }

        emit(Resource.Success(finalData))
    }

    override suspend fun syncMissingData(uid: String): Resource<Unit> {
        return try {
            // Get current local data to know what's missing
            val localPlayer = withContext(Dispatchers.IO) { localDB.playerDao().getPlayer(uid) }
            if (localPlayer == null) {
                return Resource.Error("Player not found locally")
            }

            // Fetch from Firestore
            val remoteData = fetchFullPlayerDataFromFirestore(uid)

            // Determine what to store locally
            val attributesToSave = if (remoteData?.attributes != null) {
                remoteData.attributes
            } else {
                // If remote missing, create default attributes
                createDefaultAttributes(uid)
            }

            val progressToSave = if (remoteData?.progress != null) {
                remoteData.progress
            } else {
                createDefaultProgress(uid)
            }

            val streakToSave = if (remoteData?.streak != null) {
                remoteData.streak
            } else {
                createDefaultStreak(uid)
            }

            // Update local DB
            withContext(Dispatchers.IO) {
                localDB.withTransaction {
                    localDB.playerAttributesDao().insertPlayerAttributes(attributesToSave.toEntity())
                    localDB.playerProgressDao().insertPlayerProgress(progressToSave.toEntity())
                    localDB.playerStreakDao().insertPlayerStreak(streakToSave.toEntity())
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    override suspend fun savePlayerData(playerData: PlayerData): Resource<Unit> {
        // Same as the previous savePlayerDataConcurrently but with only player data
        return try {
            val player = playerData.player ?: return Resource.Error("Player data is missing")
            val attributes = playerData.attributes ?: return Resource.Error("Attributes data is missing")
            val progress = playerData.progress ?: return Resource.Error("Progress data is missing")
            val streak = playerData.streak ?: return Resource.Error("Streak data is missing")

            supervisorScope {
                val firebaseDeferred = async {
                    runCatching { savePlayerToFirestore(playerData) }
                }

                val localDeferred = async(Dispatchers.IO) {
                    runCatching {
                        localDB.withTransaction {
                            localDB.playerDao().insertPLayer(player.toEntity())
                            localDB.playerAttributesDao().insertPlayerAttributes(attributes.toEntity())
                            localDB.playerProgressDao().insertPlayerProgress(progress.toEntity())
                            localDB.playerStreakDao().insertPlayerStreak(streak.toEntity())
                        }
                    }
                }

                val results = awaitAll(firebaseDeferred, localDeferred)
                val localOutcome = results[1]

                when {
                    localOutcome.isFailure -> {
                        Resource.Error("Failed to save locally: ${localOutcome.exceptionOrNull()?.message}")
                    }
                    else -> Resource.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Resource.Error("Failed to save player data: ${e.message}")
        }
    }

    override fun getCurrentPlayer(): Flow<Resource<Player?>> = flow {
        val user = auth.currentUser ?: run {
            emit(Resource.Success(null))
            return@flow
        }
        val localPlayer = withContext(Dispatchers.IO) {
            localDB.playerDao().getPlayer(user.uid)?.toDomain()
        }
        if (localPlayer != null) {
            emit(Resource.Success(localPlayer))
        } else {
            // Try to fetch from Firestore
            val firestorePlayer = fetchPlayerFromFirestore(user.uid)
            if (firestorePlayer != null) {
                withContext(Dispatchers.IO) {
                    localDB.playerDao().insertPLayer(firestorePlayer.toEntity())
                }
                emit(Resource.Success(firestorePlayer))
            } else {
                emit(Resource.Error("Player not found"))
            }
        }
    }

    override suspend fun awardXP(uid: String, xp: Int): Resource<Int> = withContext(Dispatchers.IO) {
        try {
            localDB.withTransaction {
                val currentProgress = localDB.playerProgressDao().getPlayerProgress(uid)?.toDomain()
                    ?: createDefaultProgress(uid)
                
                val newXP = (currentProgress.exp ?: 0) + xp
                val oldLevel = currentProgress.level ?: 1
                val newLevel = LevelCalculator.calculateLevel(newXP)
                
                var availablePoints = currentProgress.availablePoints
                if (newLevel > oldLevel) {
                    availablePoints += (newLevel - oldLevel) * LevelCalculator.POINTS_PER_LEVEL
                    Timber.tag("PlayerRepository").i("Level Up! $oldLevel -> $newLevel. Gained ${ (newLevel - oldLevel) * LevelCalculator.POINTS_PER_LEVEL} points.")
                }
                
                val updatedProgress = currentProgress.copy(
                    exp = newXP,
                    level = newLevel,
                    availablePoints = availablePoints
                )

                //localDB.playerProgressDao().insertPlayerProgress(updatedProgress.toEntity())
                localDB.playerProgressDao().updateProgress(uid, availablePoints, newLevel, newXP)
                
                Resource.Success(newLevel)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to award XP")
        }
    }

    override suspend fun syncUnsynced(): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("User not logged in")
        val uid = user.uid

        return try {
            var hasUpdates = false
            val batchWrite = db.batch()

            val localPlayer = withContext(Dispatchers.IO) { localDB.playerDao().getPlayer(uid) }
            val localAttributes = withContext(Dispatchers.IO) { localDB.playerAttributesDao().getPlayerAttributes(uid) }
            val localProgress = withContext(Dispatchers.IO) { localDB.playerProgressDao().getPlayerProgress(uid) }
            val localStreak = withContext(Dispatchers.IO) { localDB.playerStreakDao().getPlayerStreak(uid) }

            if (localPlayer?.needsSync == true) {
                batchWrite.set(db.collection("players").document(uid), localPlayer.toDomain())
                hasUpdates = true
            }
            if (localAttributes?.needSync == true) {
                batchWrite.set(db.collection("player_attributes").document(uid), localAttributes.toDomain())
                hasUpdates = true
            }
            if (localProgress?.needSync == true) {
                batchWrite.set(db.collection("player_progress").document(uid), localProgress.toDomain())
                hasUpdates = true
            }
            if (localStreak?.needSync == true) {
                batchWrite.set(db.collection("player_streaks").document(uid), localStreak.toDomain())
                hasUpdates = true
            }

            if (hasUpdates) {
                batchWrite.commit().await()
                val now = Date()
                val nowTime = now.time
                withContext(Dispatchers.IO) {
                    localDB.withTransaction {
                        if (localPlayer?.needsSync == true) localDB.playerDao().markAsSynced(uid, now)
                        if (localAttributes?.needSync == true) localDB.playerAttributesDao().updateSyncStatus(uid, false, nowTime)
                        if (localProgress?.needSync == true) localDB.playerProgressDao().updateSyncStatus(uid, false, nowTime)
                        if (localStreak?.needSync == true) {
                            localDB.playerStreakDao().updatePlayerStreak(localStreak.copy(needSync = false, lastSync = nowTime))
                        }
                    }
                }
                Timber.tag("PlayerRepository").i("✔ Nightly sync for PlayerData committed")
            } else {
                Timber.tag("PlayerRepository").i("No PlayerData to sync")
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("syncUnsynced failed: ${e.message}")
        }
    }

    private suspend fun fetchFullPlayerDataFromFirestore(uid: String): PlayerData? {
        return try {
            coroutineScope {
                val playerDoc = db.collection("players").document(uid).get().await()
                if (!playerDoc.exists()) return@coroutineScope null
                val player = playerDoc.toObject<Player>() ?: return@coroutineScope null

                val attrDeferred = async { db.collection("player_attributes").document(uid).get().await().toObject<Attributes>() }
                val progDeferred = async { db.collection("player_progress").document(uid).get().await().toObject<Progress>() }
                val streakDeferred = async { db.collection("player_streaks").document(uid).get().await().toObject<Streak>() }

                PlayerData(player, attrDeferred.await(), progDeferred.await(), streakDeferred.await())
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchPlayerFromFirestore(uid: String): Player? {
        return try {
            val document = db.collection("players").document(uid).get().await()
            if (document.exists()) document.toObject<Player>() else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun savePlayerToFirestore(playerData: PlayerData): Resource<Unit> {
        return try {
            val batchWrite = db.batch()
            val pid = playerData.player!!.uid

            val playerDocRef = db.collection("players").document(pid)
            batchWrite.set(playerDocRef, playerData.player)

            val attributesDocRef = db.collection("player_attributes").document(pid)
            batchWrite.set(attributesDocRef, playerData.attributes!!)

            val progressDocRef = db.collection("player_progress").document(pid)
            batchWrite.set(progressDocRef, playerData.progress!!)

            val streakDocRef = db.collection("player_streaks").document(pid)
            batchWrite.set(streakDocRef, playerData.streak!!)

            batchWrite.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save player data: ${e.message}")
        }
    }

    private fun createDefaultAttributes(uid: String): Attributes = Attributes(
        uid = uid,
        strength = 0,
        endurance = 0,
        intelligence = 0,
        mobility = 0,
        health = 0,
        finance = 0
    )

    private fun createDefaultProgress(uid: String): Progress = Progress(
        uid = uid,
        coins = 0,
        exp = 0,
        level = 1,
        availablePoints = 0,
        currentCategory = CategoryType.CATEGORY_BEGINNER
    )

    private fun createDefaultStreak(uid: String): Streak = Streak(
        uid = uid,
        currentStreak = 0,
        longestStreak = 0,
        lastStreakUpdate = Date()
    )
}

/*
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localDB: AppDatabase
) : PlayerRepository {

    override fun getPlayerData(): Flow<Resource<PlayerData>> = flow {
        val user = auth.currentUser ?: run {
            emit(Resource.Error("User not logged in"))
            return@flow
        }
        val uid = user.uid

        // Helper to fetch local data
        suspend fun fetchLocal(): PlayerData? = withContext(Dispatchers.IO) {
            val player = localDB.playerDao().getPlayer(uid)?.toDomain()
            if (player == null) return@withContext null
            val attributes = localDB.playerAttributesDao().getPlayerAttributes(uid)?.toDomain()
            val progress = localDB.playerProgressDao().getPlayerProgress(uid)?.toDomain()
            val streak = localDB.playerStreakDao().getPlayerStreak(uid)?.toDomain()
            PlayerData(player, attributes, progress, streak)
        }

        // Try local first
        val localData = fetchLocal()
        if (localData != null) {
            emit(Resource.Success(localData))

            // If any part is missing, trigger background sync (which will use defaults if remote also missing)
            if (localData.attributes == null || localData.progress == null || localData.streak == null) {
                // Sync in background without blocking
                // viewModelScope?.launch { syncMissingData(uid) } // You'll need to pass scope or use GlobalScope
            }
            return@flow
        }

        // No local data at all → fetch from Firestore
        emit(Resource.Loading())
        val remoteData = fetchFullPlayerDataFromFirestore(uid)

        // Create final PlayerData by filling missing fields with defaults
        val finalData = remoteData?.let {
            PlayerData(
                player = it.player,
                attributes = it.attributes ?: createDefaultAttributes(uid),
                progress = it.progress ?: createDefaultProgress(uid),
                streak = it.streak ?: createDefaultStreak(uid)
            )
        } ?: run {
            // If no remote data at all (e.g., new user without any Firestore docs), create all defaults
            PlayerData(
                player = Player(uid = uid),
                attributes = createDefaultAttributes(uid),
                progress = createDefaultProgress(uid),
                streak = createDefaultStreak(uid)
            )
        }

        // Save the final data to local DB
        withContext(Dispatchers.IO) {
            localDB.withTransaction {
                finalData.player?.let { localDB.playerDao().insertPLayer(it.toEntity()) }
                finalData.attributes?.let { localDB.playerAttributesDao().insertPlayerAttributes(it.toEntity()) }
                finalData.progress?.let { localDB.playerProgressDao().insertPlayerProgress(it.toEntity()) }
                finalData.streak?.let { localDB.playerStreakDao().insertPlayerStreak(it.toEntity()) }
            }
        }

        emit(Resource.Success(finalData))
    }

    override suspend fun syncMissingData(uid: String): Resource<Unit> {
        return try {
            // Get current local data to know what's missing
            val localPlayer = withContext(Dispatchers.IO) { localDB.playerDao().getPlayer(uid) }
            if (localPlayer == null) {
                return Resource.Error("Player not found locally")
            }

            // Fetch from Firestore
            val remoteData = fetchFullPlayerDataFromFirestore(uid)

            // Determine what to store locally
            val attributesToSave = if (remoteData?.attributes != null) {
                remoteData.attributes
            } else {
                // If remote missing, create default attributes
                createDefaultAttributes(uid)
            }

            val progressToSave = if (remoteData?.progress != null) {
                remoteData.progress
            } else {
                createDefaultProgress(uid)
            }

            val streakToSave = if (remoteData?.streak != null) {
                remoteData.streak
            } else {
                createDefaultStreak(uid)
            }

            // Update local DB
            withContext(Dispatchers.IO) {
                localDB.withTransaction {
                    localDB.playerAttributesDao().insertPlayerAttributes(attributesToSave.toEntity())
                    localDB.playerProgressDao().insertPlayerProgress(progressToSave.toEntity())
                    localDB.playerStreakDao().insertPlayerStreak(streakToSave.toEntity())
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Sync failed: ${e.message}")
        }
    }

    // Default value creators
    private fun createDefaultAttributes(uid: String): Attributes = Attributes(
        uid = uid,
        strength = 0,
        endurance = 0,
        intelligence = 0,
        mobility = 0,
        health = 0,
        finance = 0
    )

    private fun createDefaultProgress(uid: String): Progress = Progress(
        uid = uid,
        coins = 0,
        exp = 0,
        level = 1,
        availablePoints = 0,
        currentCategory = CategoryType.CATEGORY_BEGINNER
    )

    private fun createDefaultStreak(uid: String): Streak = Streak(
        uid = uid,
        currentStreak = 0,
        longestStreak = 0,
        lastStreakUpdate = Date()
    )

    // ... rest of the class (fetchFullPlayerDataFromFirestore, savePlayerToFirestore, etc.) unchanged ...
}
 */
