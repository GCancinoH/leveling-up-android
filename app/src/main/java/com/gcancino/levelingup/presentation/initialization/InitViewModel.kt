package com.gcancino.levelingup.presentation.initialization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.BodyMeasurementDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.domain.repositories.QuestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class InitViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val questRepository: QuestRepository,
    private val playerDao: PlayerDao,
    private val compositionDao: BodyCompositionDao,
    private val measurementDao: BodyMeasurementDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "InitViewModel"

    /**
     * Routing state
     * needsOnboarding = true  → route to "onboarding"
     * needsOnboarding = false → route to "dashboard"
     */
    sealed class UserState {
        object Loading : UserState()
        data class Ready(val player: Player, val needsOnboarding: Boolean) : UserState()
        data class Error(val message: String) : UserState()
    }
    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init {
        checkPlayerAuth()
    }

    fun checkPlayerAuth() {
        viewModelScope.launch(Dispatchers.IO) {
            _userState.value = UserState.Loading
            Timber.tag(TAG).d("checkPlayerAuth() started")

            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                Timber.tag(TAG).d("No authenticated user → routing to signIn")
                _userState.value = UserState.Error("Player not found")
                return@launch
            }

            val uid = firebaseUser.uid

            try {
                // ── Run quest sync + profile checks in parallel ───────────────────
                val questSyncDeferred    = async { runQuestSync() }
                val profileCheckDeferred = async { checkProfileComplete(uid) }

                questSyncDeferred.await()
                val needsOnboarding = profileCheckDeferred.await()

                Timber.tag(TAG).i(
                    "Auth check complete → uid: $uid | " +
                            "needsOnboarding: $needsOnboarding"
                )

                _userState.value = UserState.Ready(
                    player          = Player(uid = uid),
                    needsOnboarding = needsOnboarding
                )

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "checkPlayerAuth() failed")
                _userState.value = UserState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Checks whether the user has completed their profile.
     * Uses Room as the fast local cache first.
     * Falls back to Firestore only if Room has no data (new device install).
     *
     * Returns true if onboarding is needed (any condition missing).
     */
    private suspend fun checkProfileComplete(uid: String): Boolean {
        Timber.tag(TAG).d("Checking profile completeness for uid: $uid")

        // Check 1: Player record exists locally
        val hasPlayer = playerDao.getPlayer(uid) != null
        Timber.tag(TAG).d("hasPlayer (Room): $hasPlayer")

        // Check 2: Initial body composition exists locally
        val hasComposition = compositionDao.countInitialData(uid) > 0
        Timber.tag(TAG).d("hasInitialComposition (Room): $hasComposition")

        // Check 3: Initial body measurements exist locally
        val hasMeasurements = measurementDao.countInitialData(uid) > 0
        Timber.tag(TAG).d("hasInitialMeasurements (Room): $hasMeasurements")

        // Fast path — all data in Room → no Firestore calls needed
        if (hasPlayer && hasComposition && hasMeasurements) {
            Timber.tag(TAG).i("✔ Profile complete (from Room cache)")
            return false
        }

        // Slow path — Room is empty (new device) → check Firestore
        Timber.tag(TAG).d("Room cache miss → checking Firestore")
        return checkProfileCompleteFromFirestore(uid)
    }

    private suspend fun checkProfileCompleteFromFirestore(uid: String): Boolean = coroutineScope {
        val playerCheck = async {
            firestore.collection("players")
                .document(uid)
                .get().await()
                .exists()
        }
        val compositionCheck = async {
            !firestore.collection("body_composition")
                .whereEqualTo("uID", uid)
                .whereEqualTo("initialData", true)
                .limit(1)
                .get().await()
                .isEmpty
        }
        val measurementsCheck = async {
            !firestore.collection("player_measurements")
                .whereEqualTo("uID", uid)
                .whereEqualTo("initialData", true)
                .limit(1)
                .get().await()
                .isEmpty
        }

        val hasPlayer      = playerCheck.await()
        val hasComposition = compositionCheck.await()
        val hasMeasurements = measurementsCheck.await()

        Timber.tag(TAG).d(
            "Firestore check → player: $hasPlayer | composition: $hasComposition | measurements: $hasMeasurements"
        )

        !(hasPlayer && hasComposition && hasMeasurements)
    }

    private suspend fun runQuestSync() {
        try {
            val needsRefresh = dataStoreManager.needsQuestRefresh()
            if (needsRefresh) {
                val syncResult = questRepository.syncQuestsFromFirestore()
                if (syncResult is Resource.Success) {
                    dataStoreManager.updateQuestLoadedStatus()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Quest sync failed (non-fatal): ${e.message}")
            // Non-fatal — don't block routing if quest sync fails
        }
    }
}
