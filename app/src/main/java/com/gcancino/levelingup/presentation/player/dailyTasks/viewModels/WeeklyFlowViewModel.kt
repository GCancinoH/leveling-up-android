package com.gcancino.levelingup.presentation.player.dailyTasks.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.dailyTasks.ReflectionAnswer
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyEntry
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyRecap
import com.gcancino.levelingup.domain.models.identity.Objective
import com.gcancino.levelingup.domain.models.identity.TimeHorizon
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.domain.repositories.ObjectiveRepository
import com.gcancino.levelingup.domain.repositories.ReflectionRepository
import com.gcancino.levelingup.domain.useCases.identity.GetWeeklyRecapUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WeeklyFlowViewModel @Inject constructor(
    private val getWeeklyRecapUseCase: GetWeeklyRecapUseCase,
    private val reflectionRepository: ReflectionRepository,
    private val objectiveRepository: ObjectiveRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class WeeklyFlowStep {
        object Recap : WeeklyFlowStep()
        object Reflect : WeeklyFlowStep()
        object Architect : WeeklyFlowStep()
        object Completed : WeeklyFlowStep()
    }

    private val _currentStep = MutableStateFlow<WeeklyFlowStep>(WeeklyFlowStep.Recap)
    val currentStep: StateFlow<WeeklyFlowStep> = _currentStep.asStateFlow()

    private val _recapData = MutableStateFlow<Resource<WeeklyRecap>>(Resource.Loading())
    val recapData: StateFlow<Resource<WeeklyRecap>> = _recapData.asStateFlow()

    private val _answers = MutableStateFlow<List<ReflectionAnswer>>(emptyList())
    val answers: StateFlow<List<ReflectionAnswer>> = _answers.asStateFlow()

    private val _nextWeekObjectives = MutableStateFlow<List<Objective>>(emptyList())
    val nextWeekObjectives: StateFlow<List<Objective>> = _nextWeekObjectives.asStateFlow()

    init {
        loadRecap()
    }

    private fun loadRecap() {
        viewModelScope.launch {
            val uID = auth.currentUser?.uid ?: return@launch
            _recapData.value = Resource.Success(getWeeklyRecapUseCase(uID))
        }
    }

    fun nextStep() {
        _currentStep.value = when (_currentStep.value) {
            WeeklyFlowStep.Recap -> WeeklyFlowStep.Reflect
            WeeklyFlowStep.Reflect -> WeeklyFlowStep.Architect
            WeeklyFlowStep.Architect -> WeeklyFlowStep.Completed
            else -> WeeklyFlowStep.Completed
        }
    }

    fun saveWeeklyReset() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val today = LocalDate.now()
            
            val entry = WeeklyEntry(
                uID = user.uid,
                weekNumber = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                year = today.year,
                answers = _answers.value,
                alignmentScore = (_recapData.value as? Resource.Success)?.data?.identityAlignmentScore ?: 0f
            )
            
            reflectionRepository.saveWeeklyEntry(entry)
            
            // Save new objectives for next week
            _nextWeekObjectives.value.forEach { objective ->
                objectiveRepository.saveObjective(objective.copy(uID = user.uid))
            }
            
            _currentStep.value = WeeklyFlowStep.Completed
        }
    }

    fun updateAnswer(questionId: String, text: String, questionText: String) {
        val current = _answers.value.toMutableList()
        val index = current.indexOfFirst { it.questionId == questionId }
        if (index != -1) {
            current[index] = current[index].copy(answer = text)
        } else {
            current.add(ReflectionAnswer(questionId, questionText, text))
        }
        _answers.value = current
    }

    fun addObjective(title: String, roleId: String) {
        val newObjective = Objective(
            id = UUID.randomUUID().toString(),
            title = title,
            roleId = roleId,
            horizon = TimeHorizon.WEEK,
            startDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // Next week
        )
        _nextWeekObjectives.value += newObjective
    }
}
