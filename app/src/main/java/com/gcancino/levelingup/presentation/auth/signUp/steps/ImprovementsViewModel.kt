package com.gcancino.levelingup.presentation.auth.signUp.steps

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImprovementsViewModel(
    private val signUpViewModel: SignUpViewModel,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _selectedImprovements = mutableStateListOf<Improvement>()
    val selectedImprovements: List<Improvement> = _selectedImprovements

    private val _savedState = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val savedState = _savedState.asStateFlow()

    fun saveImprovementsData() {
        viewModelScope.launch {
            authRepository.saveImprovementData(_selectedImprovements).collect { resource ->
                _savedState.value = resource
            }
        }
    }

    fun toggleImprovement(improvement: Improvement) {
        if (_selectedImprovements.contains(improvement)) {
            _selectedImprovements.remove(improvement)
        } else {
            _selectedImprovements.add(improvement)
        }
    }

    fun goToNextStep() { signUpViewModel.nextStep() }

}