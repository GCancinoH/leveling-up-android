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
import kotlinx.coroutines.flow.flatMapLatest
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
    private val _dateSelector = MutableStateFlow<LocalDate>(LocalDate.now())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todaySession: StateFlow<Resource<TrainingSession?>> = _dateSelector
        .flatMapLatest { localDate ->
            flow {
                emit(Resource.Loading())
                val date = Date.from(
                    localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
                Timber.tag(TAG).d("Loading session from repository for date: $date")
                exerciseRepository.getSessionForDate(date).collect { emit(it) }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Resource.Loading()
        )

    private val hasDoneMorning = if (uID.isNotEmpty()) {
        dailyTasksRepository.observeMorningCompletedToday(uID)
    } else flowOf(false)
    private val hasDoneEvening = if (uID.isNotEmpty()) {
        dailyTasksRepository.observeEveningCompletedToday(uID)
    } else flowOf(false)
    
    val showMorningCTA: StateFlow<Boolean> = combine(
        flow {
            while(true) {
                emit(LocalDateTime.now().hour)
                kotlinx.coroutines.delay(60_000)
            }
        },
        hasDoneMorning
    ) { hour, done ->
        val morningOpen = hour in 5..11
        morningOpen && !done
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showEveningCTA: StateFlow<Boolean> = combine(
        flow {
            while(true) {
                emit(LocalDateTime.now().hour)
                kotlinx.coroutines.delay(60_000)
            }
        },
        hasDoneEvening
    ) { hour, done ->
        val eveningOpen = hour in 21..23
        eveningOpen && !done
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
     * Once sync finishes, triggers reload for the current date.
     */
    private fun syncAndLoad() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            Timber.tag(TAG).d("Sync state → Syncing (thread: ${Thread.currentThread().name})")

            when (val syncResult = exerciseRepository.syncWeek(LocalDate.now())) {
                is Resource.Success -> {
                    Timber.tag(TAG).i("Sync succeeded. Triggering session load.")
                    _syncState.value = SyncState.Success
                    _dateSelector.value = LocalDate.now()
                }
                is Resource.Error -> {
                    Timber.tag(TAG).e("Sync failed: ${syncResult.message}. Attempting local load anyway.")
                    _syncState.value = SyncState.Error(syncResult.message ?: "Sync failed")
                    _dateSelector.value = LocalDate.now()
                }
                is Resource.Loading -> { /* syncWeek is a suspend fun, won't occur */ }
            }
        }
    }

    /**
     * Called by the UI when the user selects a different date in the calendar.
     * Updates the date selector flow, which triggers flatMapLatest session loading.
     */
    fun getSessionForDate(localDate: LocalDate) {
        Timber.tag(TAG).d("getSessionForDate() called for: $localDate")
        _dateSelector.value = localDate
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