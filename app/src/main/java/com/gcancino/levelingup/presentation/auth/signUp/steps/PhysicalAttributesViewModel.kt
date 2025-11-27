package com.gcancino.levelingup.presentation.auth.signUp.steps

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.unit.Dimension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class PhysicalAttributesViewModel(
    private val signUpViewModel: SignUpViewModel,
    private val authRepository: AuthRepository
) : ViewModel() {
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var bmi by mutableStateOf("")
    var bmiInterpretation by mutableStateOf("")

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    fun onWeightChange(newWeight: String) {
        weight = newWeight
    }

    fun onHeightChange(newHeight: String) {
        height = newHeight
        calculateBMI()
    }

    private fun calculateBMI() {
        val weightValue = weight.toDoubleOrNull()
        val heightValue = height.toDoubleOrNull()

        if (weightValue != null && heightValue != null) {
            val bmiValue = (weightValue / (heightValue * heightValue))
            bmi = String.format(Locale.getDefault(), "%.1f", bmiValue)
            bmiInterpretation = interpretBMI(bmiValue)
        } else {
            bmi = ""
            bmiInterpretation = ""
        }
    }

    fun interpretBMI(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal weight"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    fun saveData() {
        viewModelScope.launch {
            authRepository.savePhysicalAttributesData(height, weight, bmi).collect { resource ->
                _saveState.value = resource
            }
        }
    }

    fun goToNextStep() { signUpViewModel.nextStep() }

    internal fun isWeightValid() : Boolean { return weight.isNotEmpty() }
    internal fun isHeightValid() : Boolean { return height.isNotEmpty() }
    internal fun isBMIValid() : Boolean { return bmi.isNotEmpty() }
}