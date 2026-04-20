package com.gcancino.levelingup.presentation.initialization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.BodyMeasurementDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.domain.logic.DailyResetManager
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltySummary
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
    private val auth: FirebaseAuth,
    private val dailyResetManager: DailyResetManager
) : ViewModel() {

    private val TAG = "InitViewModel"

    sealed class UserState {
        object Loading : UserState()
        data class Ready(
            val player: Player,
            val needsOnboarding: Boolean,
            val penalty: PenaltySummary? = null
        ) : UserState()
        data class Error(val message: String) : UserState()
    }

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    init { checkPlayerAuth() }

    fun checkPlayerAuth() {
        viewModelScope.launch(Dispatchers.IO) {
            _userState.value = UserState.Loading

            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                _userState.value = UserState.Error("Player not found")
                return@launch
            }

            val uid = firebaseUser.uid

            try {
                coroutineScope {
                    val questSync    = async { runQuestSync() }
                    val profileCheck = async { checkProfileComplete(uid) }
                    // Runs in parallel — only touches Room, no network
                    val penaltyCheck = async { runDailyReset(uid) }

                    questSync.await()
                    val needsOnboarding = profileCheck.await()
                    val penalty         = penaltyCheck.await()

                    _userState.value = UserState.Ready(
                        player          = Player(uid = uid),
                        needsOnboarding = needsOnboarding,
                        penalty         = penalty
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "checkPlayerAuth() failed")
                _userState.value = UserState.Error("Unknown error")
            }
        }
    }

    private suspend fun runDailyReset(uid: String): PenaltySummary? {
        return try {
            dailyResetManager.evaluateAndApply(uid)
        } catch (e: Exception) {
            Timber.tag(TAG).w("DailyReset failed (non-fatal): ${e.message}")
            null
        }
    }

    private suspend fun checkProfileComplete(uid: String): Boolean {
        val hasPlayer = playerDao.getPlayer(uid) != null
        val hasComposition = compositionDao.countInitialData(uid) > 0
        val hasMeasurements = measurementDao.countInitialData(uid) > 0
        if (hasPlayer && hasComposition && hasMeasurements) return false
        return checkProfileCompleteFromFirestore(uid)
    }

    private suspend fun checkProfileCompleteFromFirestore(uid: String): Boolean = coroutineScope {
        val p = async { firestore.collection("players").document(uid).get().await().exists() }
        val c = async {
            !firestore.collection("body_composition")
                .whereEqualTo("uID", uid).whereEqualTo("initialData", true)
                .limit(1).get().await().isEmpty
        }
        val m = async {
            !firestore.collection("player_measurements")
                .whereEqualTo("uID", uid).whereEqualTo("initialData", true)
                .limit(1).get().await().isEmpty
        }
        !(p.await() && c.await() && m.await())
    }

    private suspend fun runQuestSync() {
        try {
            if (dataStoreManager.needsQuestRefresh()) {
                val result = questRepository.syncQuestsFromFirestore()
                if (result is Resource.Success) dataStoreManager.updateQuestLoadedStatus()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Quest sync failed (non-fatal): ${e.message}")
        }
    }
}