package com.gcancino.levelingup.presentation.player.session

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.exercise.*
import com.gcancino.levelingup.domain.repositories.ExerciseRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@HiltViewModel
class SessionPlayerViewModel @Inject constructor(
    application: Application,
    private val exerciseRepository: ExerciseRepository,
    private val playerRepository: PlayerRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val TAG = "SessionPlayerViewModel"

    private val dateTimestamp: Long = savedStateHandle.get<Long>("date") ?: System.currentTimeMillis()

    // ─── Session ──────────────────────────────────────────────────────────────────

    private val _session = MutableStateFlow<Resource<TrainingSession?>>(Resource.Loading())
    val session: StateFlow<Resource<TrainingSession?>> = _session.asStateFlow()

    // ─── 1RM — exposed publicly so UI collects it and recomposes when it loads ─────

    private val _oneRepMaxes = MutableStateFlow<Resource<List<OneRepMax>>>(Resource.Loading())
    val oneRepMaxes: StateFlow<Resource<List<OneRepMax>>> = _oneRepMaxes.asStateFlow()

    // ─── Completed Sets ───────────────────────────────────────────────────────────

    private val _completedSets = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())
    val completedSets: StateFlow<Map<String, Set<Int>>> = _completedSets.asStateFlow()

    // ─── Progress ─────────────────────────────────────────────────────────────────

    val completedSetsCount: StateFlow<Int> = _completedSets
        .map { map -> map.values.sumOf { it.size } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val totalSetsCount: StateFlow<Int> = _session
        .map { resource ->
            (resource as? Resource.Success)?.data
                ?.blocks
                ?.sumOf { block -> block.exercises.sumOf { it.sets.size } }
                ?: 0
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── Session complete: emits true when every set is done ───────────────────────
    val isSessionComplete: StateFlow<Boolean> = combine(
        completedSetsCount,
        totalSetsCount
    ) { completed, total ->
        total in 1..completed
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── Timer ────────────────────────────────────────────────────────────────────

    private val _restTimeRemaining = MutableStateFlow(0)
    val restTimeRemaining: StateFlow<Int> = _restTimeRemaining.asStateFlow()

    // Total seconds of the current rest period — used by circular timer for progress
    private val _restTotalSeconds = MutableStateFlow(0)
    val restTotalSeconds: StateFlow<Int> = _restTotalSeconds.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private var timerService: TimerService? = null
    private var timerCollectJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.tag(TAG).d("TimerService connected")
            val service = (binder as TimerService.TimerBinder).getService()
            timerService = service
            timerCollectJob = viewModelScope.launch {
                launch { service.timerValue.collect { _restTimeRemaining.value = it } }
                launch { service.totalSeconds.collect { _restTotalSeconds.value = it } }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.tag(TAG).d("TimerService disconnected")
            timerService = null
            timerCollectJob?.cancel()
        }
    }

    // ─── Notification Permission ──────────────────────────────────────────────────
    private val prefs = getApplication<Application>()
        .getSharedPreferences("session_player_prefs", Context.MODE_PRIVATE)

    private val _hasRequestedPermission = MutableStateFlow(
        prefs.getBoolean("notification_permission_requested", false)
    )
    val hasRequestedPermission: StateFlow<Boolean> = _hasRequestedPermission.asStateFlow()

    fun markPermissionRequested() {
        _hasRequestedPermission.value = true
        prefs.edit { putBoolean("notification_permission_requested", true) }
    }

    // ─── Plate Calculator ─────────────────────────────────────────────────────────

    private val _plateCalculatorTarget = MutableStateFlow<Double?>(null)
    val plateCalculatorTarget: StateFlow<Double?> = _plateCalculatorTarget.asStateFlow()

    // ─── Init ─────────────────────────────────────────────────────────────────────

    init {
        bindTimerService()
        loadSession()
        loadOneRepMaxes()
        restoreCompletedSets()
    }

    private fun bindTimerService() {
        val intent = Intent(getApplication(), TimerService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Timber.tag(TAG).d("Binding to TimerService")
    }

    private fun loadSession() {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseRepository.getSessionForDate(Date(dateTimestamp))
                .collect { res ->
                    _session.value = res
                    if (res is Resource.Success && res.data != null) {
                        exerciseRepository.saveTodaySessionLocally(res.data)
                    }
                }
        }
    }

    private fun loadOneRepMaxes() {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseRepository.getOneRepMaxes()
                .collect { res ->
                    _oneRepMaxes.value = res
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun restoreCompletedSets() {
        viewModelScope.launch(Dispatchers.IO) {
            _session
                .filterIsInstance<Resource.Success<TrainingSession?>>()
                .mapNotNull { it.data?.id }
                .distinctUntilChanged()
                .flatMapLatest { sessionId ->
                    exerciseRepository.getLogsForSessionAsMap(sessionId)
                }
                .collect { restoredMap ->
                    if (_completedSets.value.isEmpty()) {
                        _completedSets.value = restoredMap
                    }
                }
        }
    }

    // ─── Haptic ───────────────────────────────────────────────────────────────────

    private fun triggerHaptic() {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                val vibrator = manager?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Haptic feedback failed: ${e.message}")
        }
    }

    // ─── Toggle (long press) ──────────────────────────────────────────────────────

    fun toggleSetCompleted(
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
        block: ExerciseBlock,
        setRestSeconds: Int
    ) {
        triggerHaptic()

        val currentMap  = _completedSets.value.toMutableMap()
        val currentSets = currentMap[exerciseId]?.toMutableSet() ?: mutableSetOf()
        val isMarkingCompleted = !currentSets.contains(setIndex)

        viewModelScope.launch(Dispatchers.IO) {
            if (isMarkingCompleted) {
                currentSets.add(setIndex)
                exerciseRepository.toggleSetLog(sessionId, exerciseId, setIndex, true)

                val restToUse = if (block.type == BlockType.Main) {
                    setRestSeconds
                } else {
                    val isLast = block.exercises.lastOrNull()?.id == exerciseId
                    if (isLast) block.restAfterBlock else block.restBetweenExercises
                }

                if (restToUse > 0) {
                    TimerService.startService(getApplication(), restToUse)
                }
            } else {
                currentSets.remove(setIndex)
                exerciseRepository.toggleSetLog(sessionId, exerciseId, setIndex, false)
                TimerService.stopService(getApplication())
            }

            currentMap[exerciseId] = currentSets
            _completedSets.value = currentMap
        }
    }

    // ─── Skip Rest ────────────────────────────────────────────────────────────────

    fun skipRest() {
        timerService?.stopTimer() ?: TimerService.stopService(getApplication())
        _restTimeRemaining.value = 0
        _restTotalSeconds.value  = 0
    }

    // ─── Weight Calculation ───────────────────────────────────────────────────────

    /**
     * Shared logic to find a 1RM for an exercise name based on exact or partial matching.
     */
    private fun findOneRepMaxForExercise(exerciseName: String): OneRepMax? {
        val orms = (_oneRepMaxes.value as? Resource.Success)?.data ?: return null
        val normalizedName = exerciseName.replace(" ", "").lowercase(Locale.ROOT)

        var orm = orms.find {
            it.exerciseName.replace(" ", "").lowercase(Locale.ROOT) == normalizedName
        }
        if (orm == null) {
            orm = orms.filter {
                val n = it.exerciseName.replace(" ", "").lowercase(Locale.ROOT)
                normalizedName.contains(n) || n.contains(normalizedName)
            }.maxByOrNull { it.exerciseName.length }
        }
        return orm
    }

    fun calculateWeight(exerciseName: String, intensity: Double, intensityType: IntensityType): String {
        if (intensityType != IntensityType.PERCENTAGE_1RM) return ""
        val orm = findOneRepMaxForExercise(exerciseName) ?: return "Set 1RM"
        val weight = (orm.weight * intensity) / 100.0
        return "${String.format(Locale.US, "%.1f", weight)} kg"
    }

    fun calculateWeightKg(exerciseName: String, intensity: Double, intensityType: IntensityType): Double? {
        if (intensityType != IntensityType.PERCENTAGE_1RM) return null
        val orm = findOneRepMaxForExercise(exerciseName)
        return orm?.let { (it.weight * intensity) / 100.0 }
    }

    // ─── Plate Calculator Sheet ───────────────────────────────────────────────────

    fun openPlateCalculator(targetKg: Double) {
        _plateCalculatorTarget.value = targetKg
    }

    fun finishSession(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val xpToAward = completedSetsCount.value * 10 
        
        viewModelScope.launch(Dispatchers.IO) {
            playerRepository.awardXP(uid, xpToAward)
            onComplete()
        }
    }

    fun closePlateCalculator() {
        _plateCalculatorTarget.value = null
    }

    fun calculatePlates(targetKg: Double): List<Pair<Double, Int>> {
        val barWeight   = 20.0
        val plateSizes  = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        val weightPerSide = (targetKg - barWeight) / 2.0

        if (weightPerSide <= 0) return emptyList()

        var remaining = weightPerSide
        val result    = mutableListOf<Pair<Double, Int>>()

        for (plate in plateSizes) {
            val count = (remaining / plate).toInt()
            if (count > 0) {
                result.add(Pair(plate, count))
                remaining -= plate * count
                remaining  = Math.round(remaining * 1000.0) / 1000.0 
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        timerCollectJob?.cancel()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) { }
    }
}
