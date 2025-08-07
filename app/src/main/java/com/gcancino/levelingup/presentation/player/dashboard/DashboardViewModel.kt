package com.gcancino.levelingup.presentation.player.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.repositories.BodyCompositionRepositoryImpl
import com.gcancino.levelingup.data.repositories.QuestRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val questRepository: QuestRepositoryImpl,
    private val bodyCompositionRepository: BodyCompositionRepositoryImpl
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val state = _state.asStateFlow()

    fun saveQuestsLocally() {
        viewModelScope.launch {
            Log.d("DashboardViewModel", "saveQuestsLocally called. Current state: ${_state.value}") // Log before
            _state.value = Resource.Loading() // Explicitly set to Loading before the operation
            questRepository.syncQuestsFromFirestoreDos().let { result ->
                Log.d("DashboardViewModel", "syncQuestsFromFirestore returned: $result") // <-- ADD THIS
                _state.value = result
                if (result is Resource.Success) {
                    Log.d("DashboardViewModel", "ViewModel: Quests saved locally successfully (result is Success)")
                } else if (result is Resource.Error) {
                    Log.e("DashboardViewModel", "ViewModel: Error syncing quests: ${result.message}")
                } else if (result is Resource.Loading) {
                    Log.w("DashboardViewModel", "ViewModel: Result is still Loading. Is this expected?")
                }
            }
        }
    }

}