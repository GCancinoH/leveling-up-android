package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.domain.models.dailyTasks.XPScale
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import com.gcancino.levelingup.domain.models.event.CentralEventProcessor
import com.gcancino.levelingup.domain.models.event.PlayerEvent
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val dailyRepository: DailyTasksRepository,
    private val playerRepository: PlayerRepository,
    private val eventProcessor: CentralEventProcessor,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "TasksViewModel"

    private val uID get() = auth.currentUser?.uid ?: ""

    // Temporary list of tasks being created in the BottomSheet
    private val _tasks = MutableStateFlow<List<DailyTask>>(emptyList())
    val tasks: StateFlow<List<DailyTask>> = _tasks.asStateFlow()

    // Real-time observation of already saved tasks
    val allTasks: StateFlow<List<DailyTask>> = dailyRepository
        .getTodaysTasks(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks: StateFlow<List<DailyTask>> = dailyRepository
        .getTodaysPendingTasks(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canAddMore: StateFlow<Boolean> = allTasks
        .map { it.size < 5 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Events
    private val _xpEarned = MutableSharedFlow<Int>(replay = 0)
    val xpEarned: SharedFlow<Int> = _xpEarned.asSharedFlow()

    private val _levelUp = MutableSharedFlow<Int>(replay = 0)
    val levelUp: SharedFlow<Int> = _levelUp.asSharedFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>(replay = 0)
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    private val _error = MutableSharedFlow<String>(replay = 0)
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun addTask(title: String, priority: TaskPriority) {
        val currentCount = _tasks.value.size + allTasks.value.size
        if (currentCount >= 5) return
        
        val uID = auth.currentUser?.uid ?: return
        val task = DailyTask(
            id = UUID.randomUUID().toString(),
            uID = uID,
            date = Date(),
            title = title,
            priority = priority,
            xpReward = XPScale.rewardForPriority(priority),
            isSynced = false
        )
        _tasks.value = (_tasks.value + task).sortedByDescending { it.priority }
    }

    fun removeTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }
    
    fun saveTasks() {
        val tasksToSave = _tasks.value
        if (tasksToSave.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = dailyRepository.saveTasks(tasksToSave)) {
                is Resource.Success -> {
                    Timber.tag(TAG).i("✔ Tasks saved successfully")
                    _tasks.value = emptyList() // Clear local draft
                    _saveSuccess.emit(Unit)
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Failed to save tasks"
                    Timber.tag(TAG).e(msg)
                    _error.emit(msg)
                }
                else -> Unit
            }
        }
    }

    fun completeTask(taskId: String) {
        // Find task to know reward before completing
        val task = allTasks.value.find { it.id == taskId } ?: return
        val reward = task.xpReward ?: 0

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Pass execution to CentralEventProcessor
                eventProcessor.process(PlayerEvent.TaskCompleted(taskId, uID))
                if (reward > 0) _xpEarned.emit(reward)
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to complete task")
            }
        }
    }
}
