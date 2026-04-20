package com.gcancino.levelingup.data.repositories

import androidx.room.Transaction
import androidx.room.withTransaction
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.AppDatabase
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.PlayerAttributesDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.data.mappers.isFresh
import com.gcancino.levelingup.data.mappers.toDomain
import com.gcancino.levelingup.data.mappers.toEntity
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.Attributes
import com.gcancino.levelingup.domain.models.player.CategoryType
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.domain.models.player.Progress
import com.gcancino.levelingup.domain.models.player.Streak
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val localDB: AppDatabase
) : AuthRepository {

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Resource<Player> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("User not found")

            val player = fetchAndCacheFromFirestore(user.uid)
            if (player != null) {
                Resource.Success(player)
            } else {
                Resource.Error("Failed to fetch player data")
            }
        } catch (e: FirebaseAuthException) {
            Resource.Error(getAuthErrorMessage(e.errorCode))
        } catch (e: Exception) {
            Resource.Error("Authentication failed")
        }
    }

    override fun signUpWithEmailAndPassword(
        email: String,
        password: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: run {
                emit(Resource.Error("Failed to create user"))
                return@flow
            }

            val playerData = createPlayerData(user)
            val saveResult = savePlayerDataConcurrently(playerData)
            emit(saveResult)

        } catch (e: FirebaseAuthException) {
            emit(Resource.Error(getAuthErrorMessage(e.errorCode)))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to create user: ${e.message}"))
        }
    }

    override fun savePersonalInfoData(
        name: String,
        birthdate: Date,
        gender: String
    ): Flow<Resource<PlayerData>> = flow {
        emit(Resource.Loading())

        val user = auth.currentUser ?: run {
            emit(Resource.Error("User not found"))
            return@flow
        }

        try {
            val convertedGender = Genders.fromString(gender)
            val profileUpdates = userProfileChangeRequest { displayName = name }

            // Execute Firebase and local DB updates concurrently
            coroutineScope {
                val firebaseDeferred = async {
                    runCatching { user.updateProfile(profileUpdates).await() }
                }
                val firestoreDeferred = async {
                    runCatching {
                        db.collection("players").document(user.uid).update(
                            mapOf(
                                "displayName" to name,
                                "birthDate" to birthdate,
                                "gender" to convertedGender
                            )
                        ).await()
                    }
                }
                val localDeferred = async(Dispatchers.IO) {
                    runCatching {
                        localDB.playerDao().updateLocalPersonalData(user.uid, name, birthdate, gender)
                    }
                }

                val firebaseResult = firebaseDeferred.await()
                val firestoreResult = firestoreDeferred.await()
                val localResult = localDeferred.await()

                when {
                    firebaseResult.isFailure -> {
                        emit(Resource.Error("Failed to update Firebase profile: ${firebaseResult.exceptionOrNull()?.message}"))
                        return@coroutineScope
                    }
                    firestoreResult.isFailure -> {
                        emit(Resource.Error("Failed to update Firestore: ${firestoreResult.exceptionOrNull()?.message}"))
                        return@coroutineScope
                    }
                    localResult.isFailure -> {
                        emit(Resource.Error("Failed to update local database: ${localResult.exceptionOrNull()?.message}"))
                        return@coroutineScope
                    }
                    (localResult.getOrNull() ?: 0) <= 0 -> {
                        emit(Resource.Error("Failed to save player's personal data"))
                        return@coroutineScope
                    }
                    else -> {
                        val playerData = PlayerData(
                            player = Player(
                                uid = user.uid,
                                displayName = name,
                                birthDate = birthdate,
                                gender = convertedGender
                            )
                        )
                        emit(Resource.Success(playerData))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error("Unknown error"))
        }
    }

    override fun savePhysicalAttributesData(
        height: String,
        weight: String,
        bmi: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        val user = auth.currentUser ?: run {
            emit(Resource.Error("User not found"))
            return@flow
        }

        try {
            val heightValue = height.toDoubleOrNull() ?: run {
                emit(Resource.Error("Invalid height"))
                return@flow
            }
            val weightValue = weight.toDoubleOrNull() ?: run {
                emit(Resource.Error("Invalid weight"))
                return@flow
            }
            val bmiValue = bmi.toDoubleOrNull() ?: run {
                emit(Resource.Error("Invalid BMI"))
                return@flow
            }

            val bodyCompositionData = BodyComposition(
                uID = user.uid,
                weight = weightValue,
                bmi = bmiValue,
                date = Date(),
                initialData = true
            )

            coroutineScope {
                val firebaseDeferred = async {
                    db.collection("body_composition").document(user.uid)
                        .set(bodyCompositionData)
                }
                val heightDeferred = async {
                    db.collection("players").document(user.uid).update(
                        "height", heightValue
                    )
                }
                val localDeferred = async(Dispatchers.IO) {
                    localDB.playerDao().updateLocalHeight(user.uid, heightValue)
                }
                val localBodyCompositionDeferred = async(Dispatchers.IO) {
                    /*localDB.bodyCompositionDao().insertBodyComposition(bodyCompositionData.toEntity())*/
                }

                val firebaseResult = firebaseDeferred.await()
                val heightResult = heightDeferred.await()
                val localResult = localDeferred.await()
                val localBodyCompositionResult = localBodyCompositionDeferred.await()

                when {
                    !firebaseResult.isSuccessful -> {
                        emit(Resource.Error("Failed to save player's physical data"))
                    }
                    !heightResult.isSuccessful -> {
                        emit(Resource.Error("Failed to save player's physical data"))
                    }
                    localResult <= 0 -> {
                        emit(Resource.Error("Failed to save player's physical data"))
                    }
                    /*localBodyCompositionResult <= 0 -> {
                        emit(Resource.Error("Failed to save player's physical data"))
                    }*/
                }
                emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Unknown error"))
        }
    }

    override fun saveImprovementData(improvement: List<Improvement>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val user = auth.currentUser ?: run {
                emit(Resource.Error("User not found"))
                return@flow
            }

            coroutineScope {
                val firebaseDeferred = async {
                    runCatching {
                        db.collection("players").document(user.uid)
                            .update("improvements", improvement)
                            .await()
                    }

                }
                val localDeferred = async(Dispatchers.IO) {
                    runCatching {
                        localDB.playerDao().updateLocalImprovements(user.uid, improvement)
                    }
                }

                val firebaseOutcome = firebaseDeferred.await()
                val localOutcome = localDeferred.await()

                when {
                    firebaseOutcome.isFailure -> {
                        emit(Resource.Error("Failed to save player's improvement data"))
                    }
                    localOutcome.isFailure -> {
                        emit(Resource.Error("Local save failed: ${localOutcome.exceptionOrNull()?.message}"))
                    }
                    (localOutcome.getOrNull() ?: 0) <= 0 -> {
                        emit(Resource.Error("Failed to update local improvements"))
                    }
                }
                emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Unknown error"))
        }
    }

    override suspend fun forgotPassword(email: String): Resource<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(true)
        } catch (e: FirebaseAuthException) {
            Resource.Error(getAuthErrorMessage(e.errorCode))
        } catch (e: Exception) {
            Resource.Error("Failed to send reset email")
        }
    }

    override suspend fun signOut(): Resource<Boolean> {
        return try {
            auth.signOut()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error("Failed to sign out")
        }
    }

    override fun getCurrentPlayer(): Flow<Resource<Player?>> = flow {
        try {
            val playerFB = auth.currentUser
            if (playerFB == null) {
                emit(Resource.Error("Player not found"))
                return@flow
            }

            val playerID = playerFB.uid
            emit(Resource.Loading())

            // Check local database
            val localPlayer = withContext(Dispatchers.IO) {
                localDB.playerDao().getPlayer(playerID)
            }

            when {
                localPlayer == null -> {
                    fetchAndCacheFromFirestore(playerID)?.let { player ->
                        emit(Resource.Success(player))
                    } ?: emit(Resource.Error("Failed to fetch player data"))
                }

                localPlayer.isFresh() && !localPlayer.needsSync -> {
                    emit(Resource.Success(localPlayer.toDomain()))
                }

                else -> {
                    emit(Resource.Success(localPlayer.toDomain()))

                    try {
                        fetchAndCacheFromFirestore(playerID)?.let { freshPlayer ->
                            emit(Resource.Success(freshPlayer))
                        }
                    } catch (e: Exception) {
                        // Silent failure - we already have local data
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to get current player: ${e.message}"))
        }
    }

    override fun getPlayerData(): Flow<Resource<PlayerData>> = flow {
        val user = auth.currentUser ?: run {
            emit(Resource.Error("User not logged in"))
            return@flow
        }
        val uid = user.uid
        emit(Resource.Loading())

        try {
            // 1. Fetch everything from local DB
            val localPlayerData = withContext(Dispatchers.IO) {
                val player = localDB.playerDao().getPlayer(uid)?.toDomain()
                val attributes = localDB.playerAttributesDao().getPlayerAttributes(uid)?.toDomain()
                val progress = localDB.playerProgressDao().getPlayerProgress(uid)?.toDomain()
                val streak = localDB.playerStreakDao().getPlayerStreak(uid)?.toDomain()
                
                PlayerData(player, attributes, progress, streak)
            }

            // 2. If we have local player data, emit it and STOP (Network First/Sync handled by Workers)
            if (localPlayerData.player != null) {
                emit(Resource.Success(localPlayerData))
                return@flow
            }

            // 3. Only sync with Firestore if local data is missing (e.g., new device)
            val remotePlayerData = fetchFullPlayerDataFromFirestore(uid)
            if (remotePlayerData?.player != null) {
                // Cache it
                withContext(Dispatchers.IO) {
                    localDB.withTransaction {
                        remotePlayerData.player.let { localDB.playerDao().insertPLayer(it.toEntity()) }
                        remotePlayerData.attributes?.let { localDB.playerAttributesDao().insertPlayerAttributes(it.toEntity()) }
                        remotePlayerData.progress?.let { localDB.playerProgressDao().insertPlayerProgress(it.toEntity()) }
                        remotePlayerData.streak?.let { localDB.playerStreakDao().insertPlayerStreak(it.toEntity()) }
                    }
                }
                emit(Resource.Success(remotePlayerData))
            } else {
                emit(Resource.Error("Failed to load player data from local or remote"))
            }

        } catch (e: Exception) {
            emit(Resource.Error("Error fetching player data: ${e.message}"))
        }
    }

    private suspend fun fetchFullPlayerDataFromFirestore(uid: String): PlayerData? {
        return try {
            coroutineScope {
                // Return null if the main player document doesn't exist to avoid "nullified" objects
                val playerDoc = db.collection("players").document(uid).get().await()
                if (!playerDoc.exists()) return@coroutineScope null
                
                val player = playerDoc.toObject<Player>() ?: return@coroutineScope null

                val attrDef = async { db.collection("player_attributes").document(uid).get().await().toObject<Attributes>() }
                val progDef = async { db.collection("player_progress").document(uid).get().await().toObject<Progress>() }
                val streakDef = async { db.collection("player_streaks").document(uid).get().await().toObject<Streak>() }
                
                PlayerData(player, attrDef.await(), progDef.await(), streakDef.await())
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getAuthState(): Flow<Resource<Player?>> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val cloudPlayer = auth.currentUser
            if (cloudPlayer == null) {
                val result = trySend(Resource.Success(null))
                if (result.isFailure) {
                    close(result.exceptionOrNull())
                }
            } else {
                val player = Player(
                    uid = cloudPlayer.uid,
                    email = cloudPlayer.email,
                    displayName = cloudPlayer.displayName,
                    photoURL = cloudPlayer.photoUrl?.toString(),
                )
                val result = trySend(Resource.Success(player))
                if (result.isFailure) {
                    close(result.exceptionOrNull())
                }
            }
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun createPlayerData(user: FirebaseUser) = PlayerData(
        player = Player(uid = user.uid, email = user.email, displayName = user.displayName),
        attributes = Attributes(
            uid = user.uid,
            strength = 0, endurance = 0, intelligence = 0,
            mobility = 0, health = 0, finance = 0
        ),
        progress = Progress(
            uid = user.uid,
            coins = 0,
            exp = 0,
            level = 1,
            currentCategory = CategoryType.CATEGORY_BEGINNER
        ),
        streak = Streak(
            uid = user.uid,
            currentStreak = 0,
            longestStreak = 0,
            lastStreakUpdate = Date()
        )

    )

    private suspend fun savePlayerDataConcurrently(data: PlayerData) : Resource<Unit> {
        return try {
            val player = data.player ?: return Resource.Error("Player data is missing")
            val attributes = data.attributes ?: return Resource.Error("Attributes data is missing")
            val progress = data.progress ?: return Resource.Error("Progress data is missing")
            val streak = data.streak ?: return Resource.Error("Streak data is missing")

            supervisorScope {
                val firebaseDeferred = async {
                    runCatching { savePlayerToFirestore(data) }
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
                val firestoreOutcome = results[0]
                val localOutcome = results[1]

                when {
                    localOutcome.isFailure -> {
                        // Local failure is critical - user should know
                        Resource.Error("Failed to save locally: ${localOutcome.exceptionOrNull()?.message}")
                    }
                    firestoreOutcome.isFailure -> {
                        // Firestore failure is less critical if local succeeded
                        val firestoreException = firestoreOutcome.exceptionOrNull()
                        if (firestoreException != null) {
                            // Log for debugging but don't fail the operation
                            Timber.tag("AuthRepository").w("Firestore sync failed: ${firestoreException.message}")
                        }
                        Resource.Success(Unit) // Local save succeeded
                    }
                    else -> {
                        val actualFirestoreResult = firestoreOutcome.getOrNull()
                        if (actualFirestoreResult is Resource.Error<*>) {
                            // Firestore returned error resource
                            Resource.Error("Firestore save failed: ${actualFirestoreResult.message}")
                        } else {
                            Resource.Success(Unit)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Resource.Error("Failed to save player data: ${e.message}")
        }
    }

    private suspend fun fetchAndCacheFromFirestore(uid: String): Player? {
        return try {
            val firestorePlayer = fetchPlayerFromFirestore(uid)
            firestorePlayer?.let { player ->
                // Cache with fresh metadata
                withContext(Dispatchers.IO) {
                    localDB.playerDao().insertPLayer(
                        player.toEntity().copy(
                            lastSync = Date(),
                            needsSync = false
                        )
                    )
                }
                player
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchPlayerFromFirestore(uid: String): Player? {
        return try {
            val document = db.collection("players").document(uid).get().await()
            if (document.exists()) {
                document.toObject<Player>()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun savePlayerToFirestore(playerData: PlayerData): Resource<Unit> {
        return try {
            val player = playerData.player
                ?: return Resource.Error("Player data is missing")
            val attributes = playerData.attributes
                ?: return Resource.Error("Attributes data is missing")
            val progress = playerData.progress
                ?: return Resource.Error("Progress data is missing")
            val streak = playerData.streak
                ?: return Resource.Error("Streak data is missing")

            val batchWrite = db.batch()
            val pid = player.uid

            val playerDocRef = db.collection("players").document(pid)
            batchWrite.set(playerDocRef, player)

            val attributesDocRef = db.collection("player_attributes").document(pid)
            batchWrite.set(attributesDocRef, attributes)

            val progressDocRef = db.collection("player_progress").document(pid)
            batchWrite.set(progressDocRef, progress)

            val streakDocRef = db.collection("player_streaks").document(pid)
            batchWrite.set(streakDocRef, streak)

            batchWrite.commit().await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save player data: ${e.message}")
        }
    }

    private fun getFirestoreErrorMessage(code: FirebaseFirestoreException.Code): String {
        return when (code) {
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication credentials are missing or invalid"
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission denied for this operation"
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Requested document not found"
            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "Document already exists"
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Quota exceeded or rate limited"
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service temporarily unavailable"
            else -> "Firestore error: ${code.name}"
        }
    }

    private fun getAuthErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already registered"
            "ERROR_INVALID_EMAIL" -> "Invalid email format"
            "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters"
            "ERROR_MISSING_EMAIL" -> "Email is required"
            "ERROR_MISSING_PASSWORD" -> "Password is required"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_OPERATION_NOT_ALLOWED" -> "Sign-up not enabled"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error occurred"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many requests, try again later"
            else -> "Authentication error: $errorCode"
        }
    }

    private fun Long.requirePositive(operation: String): Long {
        require(this > 0L) { "Failed to insert $operation" }
        return this
    }
}
