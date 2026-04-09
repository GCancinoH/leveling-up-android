package com.gcancino.levelingup.presentation.player.nutrition

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.identity.IdentityProfile
import com.gcancino.levelingup.domain.models.identity.StandardType
import com.gcancino.levelingup.domain.models.nutrition.MacroSummary
import com.gcancino.levelingup.domain.models.nutrition.NutritionAlignment
import com.gcancino.levelingup.domain.models.nutrition.NutritionEntry
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.gcancino.levelingup.domain.repositories.NutritionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val identityRepository: IdentityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val uID get() = auth.currentUser?.uid ?: ""

    // ─── Identity context ──────────────────────────────────────────────────────
    private val profile: StateFlow<IdentityProfile?> = identityRepository
        .observeIdentityProfile(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // The NUTRITION standards to send to Flask
    private val nutritionStandardTitles: List<String> get() =
        profile.value?.standards
            ?.filter { it.type == StandardType.NUTRITION && it.isActive }
            ?.map { it.title }
            ?: emptyList()

    // ─── Today's data ──────────────────────────────────────────────────────────
    val todayEntries: StateFlow<List<NutritionEntry>> = nutritionRepository
        .observeTodayEntries(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val todayMacros: StateFlow<MacroSummary> = nutritionRepository
        .observeTodayMacros(uID)
        .stateIn(viewModelScope, SharingStarted.Eagerly, MacroSummary(0, 0f, 0f, 0f, 0f))

    // ─── Auto-validation of NUTRITION standard ─────────────────────────────────
    // NUTRITION standard is validated when the user logs ≥1 aligned meal today.
    // Observed reactively — fires the moment a meal is saved.

    val nutritionStandardShouldValidate: StateFlow<Boolean> = nutritionRepository
        .observeAlignedCountToday(uID)
        .map { it >= 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── Analysis state ────────────────────────────────────────────────────────
    private val _analyzeState = MutableStateFlow<AnalyzeState>(AnalyzeState.Idle)
    val analyzeState: StateFlow<AnalyzeState> = _analyzeState.asStateFlow()
    sealed class AnalyzeState {
        object Idle       : AnalyzeState()
        object Analyzing  : AnalyzeState()
        data class Success(val entry: NutritionEntry) : AnalyzeState()
        data class Error(val message: String)          : AnalyzeState()
    }

    // ─── Analyze food photo ────────────────────────────────────────────────────
    fun analyzePhoto(imageUri: Uri) {
        val identityStatement = profile.value?.identityStatement ?: ""

        viewModelScope.launch(Dispatchers.IO) {
            _analyzeState.value = AnalyzeState.Analyzing

            val result = nutritionRepository.analyzeFood(
                uID                      = uID,
                imageUri                 = imageUri,
                identityStatement        = identityStatement,
                nutritionStandardTitles  = nutritionStandardTitles
            )

            _analyzeState.value = when (result) {
                is Resource.Success -> {
                    val entry = result.data!!

                    // Auto-validate NUTRITION standard if meal is aligned
                    if (entry.alignment == NutritionAlignment.ALIGNED) {
                        identityRepository.autoValidateNutrition(uID)
                    }

                    AnalyzeState.Success(entry)
                }
                is Resource.Error   -> AnalyzeState.Error(result.message ?: "Analysis failed")
                else                -> AnalyzeState.Error("Unexpected error")
            }
        }
    }

    fun resetAnalyzeState() { _analyzeState.value = AnalyzeState.Idle }
}