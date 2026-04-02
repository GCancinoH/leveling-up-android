package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.bodyComposition.BodyMeasurement
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem
import com.gcancino.levelingup.domain.models.bodyComposition.cmToInches
import com.gcancino.levelingup.domain.models.bodyComposition.inchesToCm
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
class BodyMeasurementViewModel @Inject constructor(
    private val bodyDataRepository: BodyDataRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val TAG = "BodyMeasurementViewModel"

    private val _unitSystem = MutableStateFlow(UnitSystem.METRIC)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    // Form Fields — Core
    var neck by mutableStateOf("")
    var shoulders by mutableStateOf("")
    var chest by mutableStateOf("")
    var waist by mutableStateOf("")
    var umbilical by mutableStateOf("")
    var hip by mutableStateOf("")

    // Form Fields — Arms
    var bicepLeftRelaxed by mutableStateOf("")
    var bicepLeftFlexed by mutableStateOf("")
    var bicepRightRelaxed by mutableStateOf("")
    var bicepRightFlexed by mutableStateOf("")
    var forearmLeft by mutableStateOf("")
    var forearmRight by mutableStateOf("")

    // Form Fields — Legs
    var thighLeft by mutableStateOf("")
    var thighRight by mutableStateOf("")
    var calfLeft by mutableStateOf("")
    var calfRight by mutableStateOf("")

    var isInitialData by mutableStateOf(false)

    // All fields as a flat list for validation
    private val allFields get() = listOf(
        neck, shoulders, chest, waist,
        umbilical, hip, bicepLeftRelaxed, bicepLeftFlexed,
        bicepRightRelaxed, bicepRightFlexed, forearmLeft,
        forearmRight, thighLeft, thighRight, calfLeft, calfRight
    )

    val isFormValid: Boolean
        get() = allFields.all { it.isNotBlank() && it.toDoubleOrNull() != null }

    // Save State
    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    // Unit toggle with auto-conversion
    fun toggleUnitSystem() {
        val current = _unitSystem.value
        val next    = if (current == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC

        Timber.tag(TAG).d("Toggling unit system: $current → $next")

        fun String.convert(): String {
            val v = toDoubleOrNull() ?: return this
            return if (current == UnitSystem.METRIC) v.cmToInches().formatField()
            else v.inchesToCm().formatField()
        }

        // Convert all length fields
        neck              = neck.convert()
        shoulders         = shoulders.convert()
        chest             = chest.convert()
        waist             = waist.convert()
        umbilical         = umbilical.convert()
        hip               = hip.convert()
        bicepLeftRelaxed  = bicepLeftRelaxed.convert()
        bicepLeftFlexed   = bicepLeftFlexed.convert()
        bicepRightRelaxed = bicepRightRelaxed.convert()
        bicepRightFlexed  = bicepRightFlexed.convert()
        forearmLeft       = forearmLeft.convert()
        forearmRight      = forearmRight.convert()
        thighLeft         = thighLeft.convert()
        thighRight        = thighRight.convert()
        calfLeft          = calfLeft.convert()
        calfRight         = calfRight.convert()

        _unitSystem.value = next
    }

    // Saving...
    fun save() {
        if (!isFormValid) {
            Timber.tag(TAG).w("Form is not valid")
            return
        }

        val uID = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).w("User not authenticated")
            _saveState.value = Resource.Error("User not authenticated")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()
            Timber.tag(TAG).d("Saving body measurements for user: $uID...")

            val measurement = BodyMeasurement(
                id = UUID.randomUUID().toString(),
                uID = uID,
                date = Date(),
                neck = neck.toDouble(),
                shoulders = shoulders.toDouble(),
                chest = chest.toDouble(),
                waist = waist.toDouble(),
                umbilical = umbilical.toDouble(),
                hip = hip.toDouble(),
                bicepLeftRelaxed = bicepLeftRelaxed.toDouble(),
                bicepLeftFlexed = bicepLeftFlexed.toDouble(),
                bicepRightRelaxed = bicepRightRelaxed.toDouble(),
                bicepRightFlexed = bicepRightFlexed.toDouble(),
                forearmLeft = forearmLeft.toDouble(),
                forearmRight = forearmRight.toDouble(),
                thighLeft = thighLeft.toDouble(),
                thighRight = thighRight.toDouble(),
                calfLeft = calfLeft.toDouble(),
                calfRight = calfRight.toDouble(),
                initialData = isInitialData,
                unitSystem = _unitSystem.value,
                isSynced = false
            )

            val result = bodyDataRepository.saveMeasurement(measurement)
            _saveState.value = result

            if (result is Resource.Success)
                Timber.tag(TAG).d("✔ Body measurements saved successfully")
        }
    }

    fun resetSaveState() { _saveState.value = null }

    fun lengthUnit(): String = if (_unitSystem.value == UnitSystem.METRIC) "cm" else "in"
}

private fun Double.formatField(): String = String.format(Locale.US, "%.1f", this)