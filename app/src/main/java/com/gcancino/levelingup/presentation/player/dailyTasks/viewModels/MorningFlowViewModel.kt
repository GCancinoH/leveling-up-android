package com.gcancino.levelingup.presentation.player.dailyTasks.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.domain.models.Question
import com.gcancino.levelingup.domain.models.QuestionBank
import com.gcancino.levelingup.domain.models.dailyTasks.MorningEntry
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltySummary
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.event.MorningAnswer
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.useCases.processors.ProcessMorningFlowUseCase
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
class MorningFlowViewModel @Inject constructor(
    private val dailyRepository: DailyTasksRepository,
    private val processMorningFlowUseCase: ProcessMorningFlowUseCase,
    private val auth: FirebaseAuth,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val TAG = "MorningFlowViewModel"

    // Questions for today (anchor + 2 rotating, determined by date seed)
    val questions: List<Question> = QuestionBank.getTodaysMorningQuestions()

    // Current answers — index maps to questions list
    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    val lastNightPenalty: StateFlow<PenaltySummary?> = dataStoreManager.userPreferences
        .map { prefs ->
            if (prefs.lastPenaltyXpLost > 0) {
                PenaltySummary(
                    xpLost = prefs.lastPenaltyXpLost,
                    streakLost = prefs.lastPenaltyStreakLost,
                    incompleteTasks = prefs.lastPenaltyTasksCount)
            } else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentStepIndex = MutableStateFlow(0)
    val totalSteps: Int get() = questions.size

    private val _saveState = MutableStateFlow<Resource<Int>?>(null)
    val saveState: StateFlow<Resource<Int>?> = _saveState.asStateFlow()

    init {
        viewModelScope.launch {
            val draft = dataStoreManager.getMorningDraft()
            if (draft.isNotEmpty()) {
                _answers.value = draft
                currentStepIndex.value = (draft.size).coerceAtMost(questions.size)
            }
        }
    }

    fun updateAnswer(questionId: String, answer: String) {
        _answers.value += (questionId to answer)
        viewModelScope.launch {
            dataStoreManager.saveMorningDraft(_answers.value)
        }
    }

    fun nextStep() {
        if (currentStepIndex.value < totalSteps - 1) {
            currentStepIndex.value++
        }
    }

    fun previousStep() {
        if (currentStepIndex.value > 0) currentStepIndex.value--
    }

    val isCurrentAnswerValid: StateFlow<Boolean> = combine(currentStepIndex, _answers) {
        index, currentAnswers ->
            val q = questions.getOrNull(index) ?: return@combine false
            // Note: The button enables only after 10 characters are typed
            val answerText = currentAnswers[q.id] ?: ""
            answerText.trim().length >= 10
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    //val isLastStep: Boolean get() = currentStepIndex.value == totalSteps - 1
    val isLastStep: StateFlow<Boolean> = currentStepIndex
        .map { it == totalSteps - 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Saving data to the database.
     */
    fun save(questionTexts: Map<String, String>) {
        val uID = auth.currentUser?.uid ?: run {
            _saveState.value = Resource.Error("Not authenticated"); return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()

            val entry = MorningEntry(
                id      = UUID.randomUUID().toString(),
                uID     = uID,
                date    = Date(),
                answers = questions.mapNotNull { q ->
                    _answers.value[q.id]?.let { answer ->
                        ReflectionAnswer(q.id, questionTexts[q.id] ?: "", answer)
                    }
                },
                isSynced = false
            )

            // Primero guardar la entry en Room
            val saveResult = dailyRepository.saveMorningEntry(entry)
            if (saveResult is Resource.Error) {
                _saveState.value = Resource.Error(saveResult.message ?: "Failed to save")
                return@launch
            }

            // Luego procesar via CEP (solo XP + análisis — NO duplica el save)
            val morningAnswers = questions.mapNotNull { q ->
                _answers.value[q.id]?.let { answer -> MorningAnswer(q.id, answer) }
            }

            when (val result = processMorningFlowUseCase.execute(uID, morningAnswers)) {
                is ProcessMorningFlowUseCase.Result.Success -> {
                    dataStoreManager.clearPenalty()
                    dataStoreManager.clearMorningDraft()
                    Timber.tag(TAG).i(
                        "✔ Morning flow completo → XP: ${result.xpEarned} | " +
                                "Level: ${result.newLevel}"
                    )
                    _saveState.value = Resource.Success(result.newLevel)
                }
                is ProcessMorningFlowUseCase.Result.Failure -> {
                    Timber.tag(TAG).e("ProcessMorningFlow failed: ${result.reason}")
                    _saveState.value = Resource.Error(result.reason)
                }
            }
        }
    }
}
