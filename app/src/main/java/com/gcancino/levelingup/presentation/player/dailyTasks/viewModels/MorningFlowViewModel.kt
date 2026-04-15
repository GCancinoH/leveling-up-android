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
import com.gcancino.levelingup.domain.useCases.dailyTasks.SaveMorningEntryUseCase
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
    private val saveMorningEntryUseCase: SaveMorningEntryUseCase,
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

    fun save(questionTexts: Map<String, String>) {
        val uID = auth.currentUser?.uid ?: run {
            _saveState.value = Resource.Error("Not authenticated"); return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()

            // Map answers to the Domain model
            val entry = MorningEntry(
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

            // Use the Use Case!
            val saveResult = saveMorningEntryUseCase(uID, entry)

            if (saveResult is Resource.Success) {
                // Step 2: Process the flow through the central UseCase
                val morningAnswers = questions.mapNotNull { q ->
                    _answers.value[q.id]?.let { answer ->
                        MorningAnswer(q.id, answer)
                    }
                }

                val processResult = processMorningFlowUseCase.execute(uID, morningAnswers)

                when (processResult) {
                    is ProcessMorningFlowUseCase.Result.Success -> {
                        // Clear penalty prefs after showing and successful save
                        dataStoreManager.clearPenalty()
                        dataStoreManager.clearMorningDraft()
                        Timber.tag(TAG).i(
                            "✔ Morning flow complete → XP: ${processResult.xpEarned} | " +
                                    "Level: ${processResult.newLevel} | Answers: ${processResult.answerCount}"
                        )
                        _saveState.value = Resource.Success(processResult.newLevel)
                    }
                    is ProcessMorningFlowUseCase.Result.Failure -> {
                        Timber.tag(TAG).e("ProcessMorningFlow failed: ${processResult.reason}")
                        _saveState.value = Resource.Error(processResult.reason)
                    }
                }
            } else if (saveResult is Resource.Error) {
                _saveState.value = Resource.Error(saveResult.message ?: "Failed to save")
            }
            /*_saveState.value = result

            if (result is Resource.Success) {
                // Clear penalty prefs after showing and successful save
                dataStoreManager.clearPenalty()
                dataStoreManager.clearMorningDraft()
                Timber.tag(TAG).i("✔ Morning entry saved and XP awarded. New Level: ${result.data}")
            }*/
        }
    }
}
