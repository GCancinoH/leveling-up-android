package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem
import com.gcancino.levelingup.domain.models.bodyComposition.kgToLbs
import com.gcancino.levelingup.domain.models.bodyComposition.lbsToKg
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.Locale
import java.util.UUID

@HiltViewModel
class BodyCompositionViewModel @Inject constructor(
    private val bodyDataRepository: BodyDataRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "BodyCompositionViewModel"

    // Unit System
    private val _unitSystem = MutableStateFlow(UnitSystem.METRIC)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    // Has Entry Today
    private val _hasEntryToday = MutableStateFlow(false)
    val hasEntryToday: StateFlow<Boolean> = _hasEntryToday.asStateFlow()

    // Form Fields
    var weight by mutableStateOf("")
    var bmi by mutableStateOf("")
    var bodyFatPercentage by mutableStateOf("")
    var muscleMassPercentage by mutableStateOf("")
    var visceralFat by mutableStateOf("")
    var bodyAge by mutableStateOf("")
    var isInitialData by mutableStateOf(false)

    // Validation
    val isFormValid: Boolean
        get() = listOf(
            weight, bmi, bodyFatPercentage, muscleMassPercentage,
            visceralFat, bodyAge).all { it.isNotBlank() && it.toDoubleOrNull() != null }

    // Save State
    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    // Unit toggle
    fun toggleUnitSystem() {
        val current = _unitSystem.value
        val next    = if (current == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC

        Timber.tag(TAG).d("Toggling unit system: $current → $next")

        // Convert all existing values automatically
        if (current == UnitSystem.METRIC) {
            weight = weight.toDoubleOrNull()?.kgToLbs()?.formatField() ?: weight
        } else {
            weight = weight.toDoubleOrNull()?.lbsToKg()?.formatField() ?: weight
        }
        // bmi, percentages, visceralFat, bodyAge are unit-agnostic — no conversion needed

        _unitSystem.value = next
    }

    // Saving
    fun save() {
        if (!isFormValid) {
            Timber.tag(TAG).w("save() called with invalid form")
            return
        }

        val uID = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).e("No authenticated user")
            _saveState.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()
            Timber.tag(TAG).d("Saving body composition for user: $uID")

            val composition = BodyComposition(
                id = UUID.randomUUID().toString(),
                uID = uID,
                date = Date(),
                weight = weight.toDouble(),
                bmi = bmi.toDouble(),
                bodyFatPercentage = bodyFatPercentage.toDouble(),
                muscleMassPercentage = muscleMassPercentage.toDouble(),
                visceralFat = visceralFat.toDouble(),
                bodyAge = bodyAge.toInt(),
                initialData = isInitialData,
                unitSystem = _unitSystem.value,
                isSynced = false
            )

            val result = bodyDataRepository.saveComposition(composition)
            _saveState.value = result

            if (result is Resource.Success) {
                Timber.tag(TAG).i("✔ Body composition saved locally")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }

    // Unit label helpers
    fun weightUnit(): String = if (_unitSystem.value == UnitSystem.METRIC) "kg" else "lbs"
}

private fun Double.formatField(): String = String.format(Locale.US, "%.1f", this)

