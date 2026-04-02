package com.gcancino.levelingup.presentation.auth.signIn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isPasswordVisible by mutableStateOf(false)
        private set

    private val _authState = MutableStateFlow<Resource<Player>?>(null)
    val authState: StateFlow<Resource<Player>?> = _authState.asStateFlow()

    // Methods
    fun onEmailChange(newEmail: String) { email = newEmail }
    fun onPasswordChange(newPassword: String) { password = newPassword }
    fun onPasswordVisibilityChange() { isPasswordVisible = !isPasswordVisible }

    fun signIn() {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signInWithEmailAndPassword(email, password)
            _authState.value = result
        }
    }

    fun signInWithGoogle() {
        // Implementation for Google Sign-In would go here
    }
}
