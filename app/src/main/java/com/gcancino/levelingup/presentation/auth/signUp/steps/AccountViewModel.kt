package com.gcancino.levelingup.presentation.auth.signUp.steps

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AccountStepViewModel(
    val signUpViewModel: SignUpViewModel,
    val authRepository: AuthRepository
) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var emailError by mutableStateOf<String?>(null)
        private set
    var password by mutableStateOf("")
        private set
    var passwordError by mutableStateOf<String?>(null)
        private set
    var isPasswordVisible by mutableStateOf(false)

    val emailPattern: Pattern = Patterns.EMAIL_ADDRESS

    private val _signUpState = MutableStateFlow<Resource<Unit>?>(null)
    val signUpState: StateFlow<Resource<Unit>?> = _signUpState.asStateFlow()

    fun onEmailChange(newEmail: String) {
        email = newEmail.trim()
        emailError = null
        validateEmail()
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = null
        validatePassword()
    }

    fun onPasswordVisibilityChange() {
        isPasswordVisible = !isPasswordVisible
    }

    fun createAccount() {
        viewModelScope.launch {
            authRepository.signUpWithEmailAndPassword(email, password)
                .collect { resource ->
                    _signUpState.value = resource
                    Log.d("AccountStepViewModel", "Resource: $resource")
                }
        }
    }

    fun goToNextStep() { signUpViewModel.nextStep() }

    private fun validateEmail() {
        emailError = when {
            email.isEmpty() -> "Email is required"
            !emailPattern.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePassword() {
        passwordError = when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 8 characters long"
            else -> null
        }
    }

    internal fun isValidEmail(): Boolean { return emailPattern.matcher(email).matches() && (email.isNotBlank() || email.isEmpty()) }
    internal fun isValidPassword(): Boolean { return password.length >= 6 && (password.isNotBlank() || password.isEmpty()) }
}