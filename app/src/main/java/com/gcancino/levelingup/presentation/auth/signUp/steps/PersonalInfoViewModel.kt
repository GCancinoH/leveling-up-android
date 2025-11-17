package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class PersonalInfoViewModel(
    val signUpViewModel: SignUpViewModel,
    val authRepository: AuthRepository
) {
    private val _selectedGender = MutableStateFlow<String?>(null)
    val selectedGender: StateFlow<String?> = _selectedGender

    private val _saveState = MutableStateFlow<Resource<PlayerData>?>(null)
    val saveState: StateFlow<Resource<PlayerData>?> = _saveState.asStateFlow()

    var name by mutableStateOf("")
    var birthdate by mutableStateOf<Date?>(null)
    fun onNameChange(newName: String) { name = newName }

    fun onBirthdateChange(newBirthdate: Date) {
        birthdate = newBirthdate
    }

    fun selectGender(option: String) {
        _selectedGender.value = option
    }

    fun savePersonalInfo() {
        viewModelScope.launch {
            authRepository.savePersonalInfoData(name, birthdate!!, selectedGender.value!!)
                .collect { resource ->
                    _saveState.value = resource
                }
        }
    }

    fun goToNextStep() { signUpViewModel.nextStep() }

    internal fun isNameValid(): Boolean { return name.isNotBlank() }
    internal fun isBirthdateValid(): Boolean { return birthdate != null }
    internal fun isGenderValid(): Boolean { return selectedGender.value != null }


}