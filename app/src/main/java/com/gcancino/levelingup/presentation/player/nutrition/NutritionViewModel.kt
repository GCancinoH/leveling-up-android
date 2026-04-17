package com.gcancino.levelingup.presentation.player.nutrition

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.event.CentralEventProcessor
import com.gcancino.levelingup.domain.models.event.PlayerEvent
import com.gcancino.levelingup.domain.models.identity.StandardType
import com.gcancino.levelingup.domain.models.nutrition.MacroSummary
import com.gcancino.levelingup.domain.models.nutrition.NutritionAction
import com.gcancino.levelingup.domain.models.nutrition.NutritionActionType
import com.gcancino.levelingup.domain.models.nutrition.NutritionAlignment
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import com.gcancino.levelingup.domain.models.nutrition.NutritionStandardDto
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.gcancino.levelingup.domain.repositories.NutritionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val identityRepository: IdentityRepository,
    private val eventProcessor: CentralEventProcessor,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val uID get() = auth.currentUser?.uid ?: ""

    // ─── Identity context ──────────────────────────────────────────────────────
    private val profile = identityRepository
        .observeIdentityProfile(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val nutritionStandards: List<NutritionStandardDto> get() =
        profile.value?.standards
            ?.filter { it.type == StandardType.NUTRITION && it.isActive }
            ?.map { NutritionStandardDto(id = it.id, title = it.title) }
            ?: emptyList()

    // ─── Today's data ──────────────────────────────────────────────────────────
    val todayEntries: StateFlow<List<NutritionEntry>> = nutritionRepository
        .observeTodayEntries(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayMacros: StateFlow<MacroSummary> = nutritionRepository
        .observeTodayMacros(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, MacroSummary(0, 0f, 0f, 0f, 0f))

    val dailyAlignmentScore: StateFlow<Float> = todayEntries
        .map { entries ->
            if (entries.isEmpty()) 0f
            else entries.map { it.alignmentScore }.average().toFloat()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    // Auto-validación del estándar NUTRITION
    val nutritionStandardShouldValidate: StateFlow<Boolean> = nutritionRepository
        .observeAlignedCountToday(uID)
        .map { it >= 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── Analysis state ────────────────────────────────────────────────────────
    private val _analyzeState = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: StateFlow<AnalyzeState> = _analyzeState.asStateFlow()

    sealed class AnalyzeState {
        object Idle      : AnalyzeState()
        object Analyzing : AnalyzeState()
        data class Success(val entry: NutritionEntry) : AnalyzeState()
        data class Error(val message: String)          : AnalyzeState()
    }

    // ─── Analyze food photo ────────────────────────────────────────────────────
    fun analyzePhoto(imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _analyzeState.value = AnalyzeState.Analyzing

            val result = nutritionRepository.analyzeFood(
                uID               = uID,
                imageUri          = imageUri,
                identityStatement = profile.value?.identityStatement ?: "",
                nutritionStandards = nutritionStandards
            )

            when (result) {
                is Resource.Success -> {
                    val entry = result.data!!
                    // Toda la lógica de negocio (standards, quests, tasks)
                    // pasa por el CEP — el ViewModel no toma decisiones
                    eventProcessor.process(
                        PlayerEvent.NutritionAnalyzed(nutritionEntry = entry, uID = uID)
                    )
                    _analyzeState.value = AnalyzeState.Success(entry)
                }
                is Resource.Error -> _analyzeState.value = AnalyzeState.Error(result.message ?: "")
                else              -> _analyzeState.value = AnalyzeState.Error("Unexpected error")
            }
        }
    }

    fun resetAnalyzeState() { _analyzeState.value = AnalyzeState.Idle }
}