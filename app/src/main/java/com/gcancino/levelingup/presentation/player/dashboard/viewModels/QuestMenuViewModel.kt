package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import android.util.Log
import androidx.glance.unit.Dimension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.QuestDao
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.repositories.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class QuestMenuViewModel @Inject constructor(
    private val repository: QuestRepository
) : ViewModel() {

    private val _quests = MutableStateFlow<List<Quests>>(emptyList())
    val quests: StateFlow<List<Quests>> = _quests.asStateFlow()

    private val _updateQuestStatus = MutableStateFlow<Resource<Unit>?>(null)
    val updateQuestStatus: StateFlow<Resource<Unit>?> = _updateQuestStatus.asStateFlow()

    init {
        loadNotStartedQuests()
    }

    fun loadNotStartedQuests() {
        viewModelScope.launch {
            try {
                repository.getNotStartedQuests().collect { fetchedQuests ->
                    Timber.tag("QuestMenuViewModel").d("Fetched Quests: $fetchedQuests")
                    _quests.value = fetchedQuests
                }
            } catch (e: Exception) {
                _quests.value = emptyList()
            }
        }
    }

    fun updateQuestStatus(questID: String) {
        _updateQuestStatus.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateQuestStatus(questID)
            _updateQuestStatus.value = result

            if (result is Resource.Success) {
                Timber.tag("QuestMenuViewModel").d("Quest status updated successfully")
            } else {
                Timber.tag("QuestMenuViewModel").e("Failed to update quest status")

            }

        }
    }

    fun resetUpdateStatus() {
        _updateQuestStatus.value = null
    }
}