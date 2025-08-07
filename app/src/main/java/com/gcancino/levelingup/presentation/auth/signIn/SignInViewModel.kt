package com.gcancino.levelingup.presentation.auth.signIn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SignInViewModel(
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

    fun signIn() { TODO() }
    fun signInWithGoogle() { TODO() }
}