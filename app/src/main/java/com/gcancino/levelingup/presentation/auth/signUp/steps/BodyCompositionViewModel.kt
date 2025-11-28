package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.BodyComposition
import com.gcancino.levelingup.domain.repositories.BodyCompositionRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BodyCompositionViewModel(
    private val signUpViewModel: SignUpViewModel,
    private val repository: BodyCompositionRepository
) : ViewModel() {
    var fatPercentage by mutableStateOf("")
    var musclePercentage by mutableStateOf("")
    var visceralFat by mutableStateOf("")
    var bodyAge by mutableStateOf("")

    private val _savedState = MutableStateFlow<Resource<Unit>?>(null)
    val savedState = _savedState.asStateFlow()

    fun onFatPercentageChange(value: String) { fatPercentage = value }

    fun onMusclePercentageChange(value: String) { musclePercentage = value }

    fun onVisceralFatChange(value: String) { visceralFat = value }
    fun onBodyAgeChange(value: String) { bodyAge = value }

    fun saveBodyComposition() {
        val data = BodyComposition(
            uid = auth.currentUser!!.uid,
            bodyFat = fatPercentage.toDouble(),
            muscleMass = musclePercentage.toDouble(),
            visceralFat = visceralFat.toInt(),
            bodyAge = bodyAge.toInt()
        )
        viewModelScope.launch {
            repository.saveInitialBodyComposition(data).collect { result ->
                _savedState.value = result
            }
        }
    }

    fun goToNextStep() { signUpViewModel.nextStep() }
}