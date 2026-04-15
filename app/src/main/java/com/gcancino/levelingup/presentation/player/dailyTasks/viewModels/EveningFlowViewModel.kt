package com.gcancino.levelingup.presentation.player.dailyTasks.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.domain.models.Question
import com.gcancino.levelingup.domain.models.QuestionBank
import com.gcancino.levelingup.domain.models.dailyTasks.DailyTask
import com.gcancino.levelingup.domain.models.dailyTasks.EveningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.dailyTasks.TaskPriority
import com.gcancino.levelingup.domain.models.dailyTasks.XPScale
import com.gcancino.levelingup.domain.models.event.EveningAnswer
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.useCases.processors.ProcessEveningFlowUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EveningFlowViewModel @Inject constructor(
    private val dailyRepository: DailyTasksRepository,
    private val processEveningFlowUseCase: ProcessEveningFlowUseCase,
    private val dataStoreManager: DataStoreManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "EveningFlowViewModel"

    val questions: List<Question> = QuestionBank.getTodaysEveningQuestions()

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _tasks = MutableStateFlow<List<DailyTask>>(emptyList())
    val tasks: StateFlow<List<DailyTask>> = _tasks.asStateFlow()

    val currentStepIndex = MutableStateFlow(0)

    enum class EveningStep { QUESTIONS, TASKS }

    val currentPhase: EveningStep
        get() = if (currentStepIndex.value < questions.size) EveningStep.QUESTIONS
        else EveningStep.TASKS

    val totalSteps: Int get() = questions.size + 1 

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    init {
        // Restore draft on initialization
        viewModelScope.launch {
            val draft = dataStoreManager.getEveningDraft()
            if (draft.isNotEmpty()) {
                _answers.value = draft
                // Position the user at the appropriate step based on draft progress
                currentStepIndex.value = draft.size.coerceAtMost(questions.size)
            }
        }
    }

    fun updateAnswer(questionId: String, answer: String) {
        _answers.value += (questionId to answer)
        // Auto-save draft
        viewModelScope.launch {
            dataStoreManager.saveEveningDraft(_answers.value)
        }
    }

    fun nextStep() {
        if (currentStepIndex.value < totalSteps - 1) currentStepIndex.value++
    }

    fun previousStep() {
        if (currentStepIndex.value > 0) currentStepIndex.value--
    }

    val isCurrentAnswerValid: StateFlow<Boolean> = combine(currentStepIndex, _answers) {
            index, currentAnswers ->
        val q = questions.getOrNull(index) ?: return@combine true // Tasks step doesn't use this validation logic
        val answerText = currentAnswers[q.id] ?: ""
        answerText.trim().length >= 10
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isLastStep: StateFlow<Boolean> = currentStepIndex
        .map { index -> index == totalSteps - 1 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun addTask(title: String, priority: TaskPriority) {
        if (_tasks.value.size >= 5) return 
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
        _tasks.value = (_tasks.value + task).sortedBy { it.priority }
    }

    fun removeTask(taskId: String) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
    }

    fun reorderTask(taskId: String, newPriority: TaskPriority) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(
                priority = newPriority,
                xpReward = XPScale.rewardForPriority(newPriority)
            ) else task
        }.sortedBy { it.priority }
    }

    val canAddMoreTasks: StateFlow<Boolean> = _tasks.map { it.size < 5 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun save(questionTexts: Map<String, String>) {
        val uID = auth.currentUser?.uid ?: run {
            _saveState.value = Resource.Error("Not authenticated"); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()

            val entry = EveningEntry(
                id = UUID.randomUUID().toString(),
                uID = uID,
                date = Date(),
                answers = questions.mapNotNull { q ->
                    _answers.value[q.id]?.let { answer ->
                        ReflectionAnswer(q.id, questionTexts[q.id] ?: "", answer)
                    }
                },
                isSynced = false
            )

            val entryResult = dailyRepository.saveEveningEntry(entry)
            val tasksResult = if (_tasks.value.isNotEmpty()) {
                dailyRepository.saveTasks(_tasks.value)
            } else Resource.Success(Unit)

            // Check for errors in save operations
            if (entryResult is Resource.Error) {
                _saveState.value = entryResult
                return@launch
            }

            if (tasksResult is Resource.Error) {
                _saveState.value = tasksResult
                return@launch
            }

            // Step 3: Process the flow through the central UseCase
            val eveningAnswers = questions.mapNotNull { q ->
                _answers.value[q.id]?.let { answer ->
                    EveningAnswer(q.id, answer)
                }
            }

            when (val processResult = processEveningFlowUseCase.execute(uID, eveningAnswers)) {
                is ProcessEveningFlowUseCase.Result.Success -> {
                    dataStoreManager.clearEveningDraft()
                    Timber.tag(TAG).i(
                        "✔ Evening flow complete → XP: ${processResult.xpEarned} | " +
                                "Level: ${processResult.newLevel} | Quality: ${processResult.reflectionQuality} | " +
                                "Tasks: ${_tasks.value.size}"
                    )
                    _saveState.value = Resource.Success(Unit)
                }
                is ProcessEveningFlowUseCase.Result.Failure -> {
                    Timber.tag(TAG).e("ProcessEveningFlow failed: ${processResult.reason}")
                    _saveState.value = Resource.Error(processResult.reason)
                }
            }

            /*val finalResult = entryResult as? Resource.Error
                ?: (tasksResult as? Resource.Error ?: Resource.Success(Unit))
            
            _saveState.value = finalResult
            
            if (finalResult is Resource.Success) {
                dataStoreManager.clearEveningDraft()
                Timber.tag(TAG).i("✔ Evening entry + ${_tasks.value.size} task(s) saved")
            }*/
        }
    }
}
