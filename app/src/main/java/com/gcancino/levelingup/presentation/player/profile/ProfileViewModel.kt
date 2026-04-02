package com.gcancino.levelingup.presentation.player.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.repositories.AuthRepositoryImpl
import com.gcancino.levelingup.data.repositories.PlayerRepositoryImpl
import com.gcancino.levelingup.domain.models.player.PlayerData
import com.gcancino.levelingup.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepositoryImpl,
    private val playerRepository: PlayerRepositoryImpl
) : ViewModel() {

    private val _playerData = MutableStateFlow<Resource<PlayerData>>(Resource.Loading())
    val playerData: StateFlow<Resource<PlayerData>> = _playerData.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            playerRepository.getPlayerData().collect { resource ->
                _playerData.value = resource
                if (resource is Resource.Success && resource.data != null) {
                    val data = resource.data
                    if (data.attributes == null || data.progress == null || data.streak == null) {
                        syncMissingData(data.player?.uid)
                    }
                }
            }
        }
    }

    private fun syncMissingData(uid: String?) {
        if (uid == null) return
        viewModelScope.launch {
            val result = playerRepository.syncMissingData(uid)
            if (result is Resource.Success) {
                // Refresh after sync
                loadProfile()
            } else {
                Timber.e("Sync failed: ${result.message}")
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signOut()
            if (result is Resource.Success) {
                onSuccess()
            }
        }
    }
}
