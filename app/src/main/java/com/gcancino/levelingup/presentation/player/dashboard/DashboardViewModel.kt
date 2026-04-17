package com.gcancino.levelingup.presentation.player.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.core.SyncState
import com.gcancino.levelingup.data.repositories.DailyTasksRepositoryImpl
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.domain.models.dailyTasks.XPScale
import com.gcancino.levelingup.domain.models.exercise.TrainingSession
import com.gcancino.levelingup.domain.repositories.ExerciseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    dailyTasksRepository: DailyTasksRepositoryImpl,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "DashboardViewModel"

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val uID: String by lazy {
        auth.currentUser?.uid ?: run {
            _authError.value = "User not authenticated"
            ""
        }
    }

    // Sync State
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Session State
    private val _todaySession = MutableStateFlow<Resource<TrainingSession?>>(Resource.Loading())
    val todaySession: StateFlow<Resource<TrainingSession?>> = _todaySession.asStateFlow()

    private val _timeWindowTrigger = MutableStateFlow(0)
    private val hasDoneMorning = if (uID.isNotEmpty()) {
        dailyTasksRepository.observeMorningCompletedToday(uID)
    } else flowOf(false)
    private val hasDoneEvening = if (uID.isNotEmpty()) {
        dailyTasksRepository.observeEveningCompletedToday(uID)
    } else flowOf(false)
    
    val showMorningCTA: StateFlow<Boolean> = combine(
        _timeWindowTrigger,
        hasDoneMorning
    ) { _, done ->
        val hour = LocalDateTime.now().hour
        val morningOpen = hour in 5..11
        morningOpen && !done
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showEveningCTA: StateFlow<Boolean> = combine(
        _timeWindowTrigger,
        hasDoneEvening
    ) { _, done ->
        val hour = LocalDateTime.now().hour
        val eveningOpen = hour in 21..23
        eveningOpen && !done
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _showTaskCreationSheet = MutableStateFlow(false)
    val showTaskCreationSheet: StateFlow<Boolean> = _showTaskCreationSheet.asStateFlow()

    init {
        Timber.tag(TAG).d("ViewModel initialized. Starting week sync...")
        
        if (uID.isNotEmpty()) {
            syncAndLoad()
        } else {
            Timber.tag(TAG).w("User not authenticated, skipping sync")
        }
    }

    /**
     * Runs sync on Dispatchers.IO to avoid blocking the main thread.
     * Once sync finishes, loads today's session from Room.
     */
    private fun syncAndLoad() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            Timber.tag(TAG).d("Sync state → Syncing (thread: ${Thread.currentThread().name})")

            when (val syncResult = exerciseRepository.syncWeek(LocalDate.now())) {
                is Resource.Success -> {
                    Timber.tag(TAG).i("Sync succeeded. Loading today's session from Room.")
                    _syncState.value = SyncState.Success
                    loadSessionForDate(LocalDate.now())
                }
                is Resource.Error -> {
                    Timber.tag(TAG).e("Sync failed: ${syncResult.message}. Attempting local load anyway.")
                    _syncState.value = SyncState.Error(syncResult.message ?: "Sync failed")
                    loadSessionForDate(LocalDate.now())
                }
                is Resource.Loading -> { /* syncWeek is a suspend fun, won't occur */ }
            }
        }
    }

    /**
     * Called by the UI when the user selects a different date in the calendar.
     * Converts LocalDate → Date and reads from Room.
     */
    fun getSessionForDate(localDate: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.tag(TAG).d("getSessionForDate() called for: $localDate")
            loadSessionForDate(localDate)
        }
    }

    /** Internal: collects the session Flow for a given date and pushes to state. */
    private suspend fun loadSessionForDate(localDate: LocalDate) {
        val date = Date.from(
            localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
        Timber.tag(TAG).d("Loading session from Room for date: $date")

        exerciseRepository.getSessionForDate(date)
            .flowOn(Dispatchers.IO)
            .collect { resource ->
                _todaySession.value = resource
                when (resource) {
                    is Resource.Success ->
                        Timber.tag(TAG).i(
                            "Session loaded → %s", if (resource.data != null)
                                        "id: ${resource.data.id}, name: ${resource.data.name}"
                                    else "no session (Rest Day)"
                        )
                    is Resource.Error ->
                        Timber.tag(TAG).d("Failed to load session: ${resource.message}")
                    is Resource.Loading ->
                        Timber.tag(TAG).d("Loading session...")
                }
            }
    }

    /* Open and close bottom sheet: Daily Tasks */
    fun openTaskCreation() { _showTaskCreationSheet.value = true }
    fun closeTaskCreation() { _showTaskCreationSheet.value = false }

    /** Manual refresh — e.g. pull-to-refresh. */
    fun refresh() {
        Timber.tag(TAG).d("Manual refresh triggered.")
        syncAndLoad()
    }
}