package com.gcancino.levelingup.presentation.initialization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.domain.repositories.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InitUIState(
    val message: String = "",
)

@HiltViewModel
class InitViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager,
    private val questRepository: QuestRepository
) : ViewModel() {
    private val _userState = MutableStateFlow<Resource<Player>>(Resource.Loading())
    val userState: StateFlow<Resource<Player>> = _userState.asStateFlow()

    init {
        checkPlayerAuth()
    }

    private fun checkPlayerAuth() {
        viewModelScope.launch {
            _userState.update { Resource.Loading() }
            val result = auth.currentUser
            if (result == null) {
                _userState.update { Resource.Error("Player not found") }
                return@launch
            }

            try {
                val needsRefresh = dataStoreManager.needsQuestRefresh()

                if (needsRefresh) {
                     val syncResult = questRepository.syncQuestsFromFirestore()
                    if (syncResult is Resource.Success) {
                        dataStoreManager.updateQuestLoadedStatus()
                    }
                }

                _userState.value = Resource.Success(Player(result.uid))
            } catch (e: Exception) {
                _userState.value = Resource.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}
